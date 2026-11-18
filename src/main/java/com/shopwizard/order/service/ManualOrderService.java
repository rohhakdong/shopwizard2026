package com.shopwizard.order.service;

import com.shopwizard.company.model.Chnl;
import com.shopwizard.company.service.ChnlService;
import com.shopwizard.order.mapper.OrderNoSeqMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.CheckoutProdItem;
import com.shopwizard.order.model.ManualOrderRequest;
import com.shopwizard.order.model.ManualOrderResult;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderNoSeq;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.model.PricedLine;
import com.shopwizard.order.model.PricedOrder;
import com.shopwizard.product.model.Prod;
import com.shopwizard.product.service.ProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자가 직접 등록하는 주문(주문관리 화면의 "주문등록") — 오픈마켓·전화 주문을 옮겨 적는 용도.
 * 레거시 {@code OrderServiceImpl#runInsertOrderFromManual} 을 이식하되 다음을 바꿨다:
 * 상품 여러 줄, 상품·옵션·재고는 쇼핑몰 체크아웃과 같은 규칙({@link CheckoutPricingService})으로 검사, 재고는 원장 우선 차감(부족하면 전체 롤백),
 * 채널주문번호 중복 등록 방지, 공급가·매입가·마진은 상품 마스터 기준({@link ProdMatchService#backfillLine}).
 *
 * <p>주문은 항상 "지불완료"이고 결제 행은 "수기"(PayType {@value #PAY_TYPE_MANUAL}, PgCode {@value #PG_CODE_MANUAL})다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ManualOrderService {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** wizardn.tCodConstrVal(cPayType) 의 "수기". */
    static final String PAY_TYPE_MANUAL = "M00000000000";
    static final String PG_CODE_MANUAL = "MANUAL";
    static final String STATE_PAID = "지불완료";

    private final OrderNoSeqMapper orderNoSeqMapper;
    private final OrderService orderService;
    private final OrderProdService orderProdService;
    private final OrderPayService orderPayService;
    private final StockLedgerService stockLedgerService;
    private final CheckoutPricingService pricingService;
    private final ProdService prodService;   // com.shopwizard.product.service.ProdService (판매 상품)
    private final ChnlService chnlService;
    private final OrderProdMapper orderProdMapper;

    public ManualOrderResult create(ManualOrderRequest req) {
        // ── 1. 검증 — DB 에 쓰기 전에 끝낸다 ────────────────────────────────
        String chnlCode = trim(req.getChnlCode());
        if (chnlCode == null) throw bad("채널을 선택하세요.");
        Chnl chnl = chnlService.get(new HashMap<>(Map.of("chnlCode", chnlCode)));
        if (chnl == null) throw bad("존재하지 않는 채널입니다.");

        String orderName = required(req.getOrderName(), "주문자 이름을 입력하세요.");
        String orderPhone = trim(req.getOrderPhoneNo());
        String orderMobile = trim(req.getOrderMobileNo());
        if (orderPhone == null && orderMobile == null) throw bad("주문자 연락처를 입력하세요.");
        String recverName = required(req.getRecverName(), "수취인 이름을 입력하세요.");
        String recverPhone = trim(req.getRecverPhoneNo());
        String recverMobile = trim(req.getRecverMobileNo());
        if (recverPhone == null && recverMobile == null) throw bad("수취인 연락처를 입력하세요.");
        String recverAddr1 = required(req.getRecverAddr1(), "수취인 주소를 입력하세요.");

        String registId = trim(req.getRegistId()) != null ? trim(req.getRegistId()) : "MANUAL";
        String registName = trim(req.getRegistName()) != null ? trim(req.getRegistName()) : registId;

        String chnlOrderNo = trim(req.getChnlOrderNo());
        if (chnlOrderNo != null && orderProdMapper.countByChnlOrderNo(chnlCode, chnlOrderNo) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 채널주문번호입니다: " + chnlOrderNo);
        }

        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.format(DATETIME_FMT);
        String reciptDate = reciptDate(req.getOrderReciptDate(), now);

        List<ManualOrderRequest.Line> lines = req.getLines();
        if (lines == null || lines.isEmpty()) throw bad("주문 상품을 추가하세요.");

        // 상품·옵션·판매중지·재고 검사와 기본 단가 — 쇼핑몰 체크아웃과 같은 규칙. 화면에서 고친 가격은 아래에서 덮어쓴다.
        List<CheckoutProdItem> items = new ArrayList<>();
        for (ManualOrderRequest.Line l : lines) {
            if (l == null) throw bad("주문 상품 정보가 올바르지 않습니다.");
            CheckoutProdItem it = new CheckoutProdItem();
            it.setProdCode(l.getProdCode());
            it.setItemCode(l.getItemCode());
            it.setProdQty(l.getProdQty());
            items.add(it);
        }
        PricedOrder priced = pricingService.price(items);

        List<OrderProd> ops = new ArrayList<>();
        long total = 0;
        try {
            for (int i = 0; i < lines.size(); i++) {
                ManualOrderRequest.Line in = lines.get(i);
                PricedLine pl = priced.getLines().get(i);
                int qty = pl.getProdQty();

                int salePrice = in.getSalePrice() != null ? in.getSalePrice() : pl.getSalePrice();
                int optionPrice = in.getOptionPrice() != null ? in.getOptionPrice() : pl.getOptionPrice();
                if (salePrice < 0 || optionPrice < 0) throw bad("가격은 0원 이상이어야 합니다: " + pl.getProdName());
                int listPrice = pl.getListPrice() > 0 ? pl.getListPrice() : salePrice;

                Prod prod = findProd(pl.getProdCode());
                if (prod == null) throw bad("존재하지 않는 상품입니다: " + pl.getProdCode());

                OrderProd op = new OrderProd();
                op.setOrderProdNo(i + 1);
                op.setProdName(pl.getProdName());
                op.setItemCode(pl.getItemCode());
                op.setItemName(pl.getItemName());
                op.setProdQty(qty);
                // 주문 라인 금액은 모든 채널에서 "수량이 곱해진 라인 합계"다. 옵션가 = NvPrice, 판매가 = SalePrice + NvPrice.
                op.setListPrice(Math.multiplyExact(listPrice, qty));
                op.setSalePrice(Math.multiplyExact(salePrice, qty));
                op.setNvPrice(Math.multiplyExact(optionPrice, qty));
                op.setReciptAmt(Math.multiplyExact(Math.addExact(salePrice, optionPrice), qty));
                op.setPromotFeeAmt(0);
                op.setDeliFeeType(prod.getDeliFeeType());
                op.setDeliFeeAmt(0);
                op.setBundleYn(0);
                op.setAccntId("0");
                op.setChnlOrderNo(chnlOrderNo);
                op.setOrderState(STATE_PAID);
                op.setState(1);
                op.setRegistId(registId);
                op.setRegistName(registName);
                // 공급사·상점·제조사·세율과 공급가·매입가(단가×수량)·마진을 상품 마스터 기준으로 채운다.
                ProdMatchService.backfillLine(op, prod);

                total = Math.addExact(total, op.getReciptAmt());
                ops.add(op);
            }
            if (total > Integer.MAX_VALUE) throw bad("주문 금액이 너무 큽니다.");
        } catch (ArithmeticException e) {
            throw bad("주문 금액이 너무 큽니다.");
        }

        // ── 2. 저장 ────────────────────────────────────────────────────────
        OrderNoSeq seq = new OrderNoSeq();
        orderNoSeqMapper.insert(seq);
        int orderNo = seq.getSeq();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setCustId(0);
        order.setLoginId("");
        order.setOrderName(orderName);
        order.setOrderEmail(trim(req.getOrderEmail()));
        order.setOrderPhoneNo(orderPhone);
        order.setOrderMobileNo(orderMobile);
        order.setOrderZipcode("");
        order.setOrderAddr1("");
        order.setOrderAddr2("");
        order.setRecverName(recverName);
        order.setRecverPhoneNo(recverPhone);
        order.setRecverMobileNo(recverMobile);
        order.setRecverZipcode(trim(req.getRecverZipcode()));
        order.setRecverAddr1(recverAddr1);
        order.setRecverAddr2(nz(req.getRecverAddr2()).trim());   // NULL 이 아니라 '' — 출고지시가 CONCAT 으로 주소를 합칠 때 NULL 이 섞이면 전체가 NULL
        order.setMoneyUnit("KRW");
        order.setDeliMemo(trim(req.getDeliMemo()));
        order.setOrderState(STATE_PAID);
        // 주문접수일 — 주문 조회·출고지시·반품·정산의 기본 기간 조건이라 비워 두면 날짜로 검색하는 화면에서 빠진다.
        order.setOrderReciptDate(reciptDate);
        order.setPayCmpletDate(nowStr);
        order.setChnlCode(chnlCode);
        order.setRegistId(registId);
        order.setRegistName(registName);
        orderService.insert(order);

        for (OrderProd op : ops) {
            op.setOrderNo(orderNo);
            orderProdService.insert(op);
        }

        // 재고 차감 — 주문 저장과 같은 트랜잭션. 차감 이력(원장)을 먼저 선점하고 조건부로 차감하므로 부족하면 409 로
        // 주문 저장 전체가 롤백된다. 데드락을 피하려고 상품코드 순으로 처리한다.
        List<OrderProd> byProd = new ArrayList<>(ops);
        byProd.sort(Comparator.comparing(OrderProd::getProdCode));
        for (OrderProd op : byProd) {
            stockLedgerService.deductForOrder(orderNo, op.getOrderProdNo(), op.getProdCode(), op.getProdQty(), registId);
        }

        // 결제 행 — "수기". PayNo 는 관리자 조회 쿼리가 OPay.PayNo = 1 을 대표 결제건으로 보기 때문에 1.
        OrderPay pay = new OrderPay();
        pay.setOrderNo(orderNo);
        pay.setPayNo(1);
        pay.setPayType(PAY_TYPE_MANUAL);
        pay.setPayDate(nowStr);
        pay.setPayAmt((int) total);
        pay.setApprovDate(nowStr);
        pay.setReciptName(orderName);
        pay.setPgCode(PG_CODE_MANUAL);
        String ref = chnlOrderNo != null ? chnlOrderNo : String.valueOf(orderNo);
        pay.setPgOrderNo(ref);
        pay.setPgTradeNo(ref);
        pay.setApprovNo(ref);
        pay.setRegistId(registId);
        pay.setRegistName(registName);
        orderPayService.insert(pay);

        return new ManualOrderResult(orderNo, ops.size(), total);
    }

    /** 상품코드가 정확히 같은 판매 상품. 목록 조회는 상점상품코드·IMPA 코드로도 맞으므로 정확 일치만 고른다. */
    private Prod findProd(String prodCode) {
        Map<String, Object> p = new HashMap<>();
        p.put("pProdCode", prodCode);
        List<Prod> list = prodService.selectList(p);
        if (list == null) return null;
        for (Prod prod : list) if (prodCode.equals(prod.getProdCode())) return prod;
        return null;
    }

    /** 주문일자 — 비면 지금. 날짜(yyyy-MM-dd)만 받고 시각은 지금 시각을 붙인다. */
    private static String reciptDate(String raw, LocalDateTime now) {
        String s = trim(raw);
        if (s == null) return now.format(DATETIME_FMT);
        try {
            return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s).atTime(now.toLocalTime()).format(DATETIME_FMT);
        } catch (DateTimeParseException e) {
            throw bad("주문일자 형식이 올바르지 않습니다. (yyyy-MM-dd)");
        }
    }

    private static String required(String v, String message) {
        String t = trim(v);
        if (t == null) throw bad(message);
        return t;
    }

    private static String trim(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
