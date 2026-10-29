package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderNoSeqMapper;
import com.shopwizard.order.mapper.OrderPayLogTossMapper;
import com.shopwizard.order.model.CheckoutProdItem;
import com.shopwizard.order.model.CheckoutRequest;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderNoSeq;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.order.model.OrderPayLogToss;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.payment.model.PaymentResult;
import com.shopwizard.payment.service.TossGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 고객 쇼핑몰(shop.html) 체크아웃 완료 처리.
 * 토스 결제 승인 결과를 받아 tOrdOrder / tOrdOrderProd / tOrdOrderPay / tOrdOrderPayLogToss 에
 * 하나의 트랜잭션으로 저장한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class CheckoutService {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** tOrdOrderPay.PayType 값 (wizardn.tCodConstrVal, ConstrCode='cPayType' 코드값) */
    private static final Map<String, String> PAY_TYPE_CODE = Map.of(
            "card",  "100000000000",  // 신용카드
            "trans", "010000000000",  // 계좌이체
            "vbank", "001000000000"   // 가상계좌
    );

    /** shop.html(자사 쇼핑몰) 체크아웃 주문에 부여할 채널코드 */
    @Value("${shop.chnl-code}")
    private String shopChnlCode;

    private final OrderNoSeqMapper orderNoSeqMapper;
    private final OrderService orderService;
    private final OrderProdService orderProdService;
    private final OrderPayService orderPayService;
    private final OrderPayLogTossMapper orderPayLogTossMapper;
    private final StockLedgerService stockLedgerService;

    /** 같은 PG 의 같은 가맹점 주문번호로 이미 저장된 주문이 있으면 그 주문번호(재전송·새로고침 때 중복 주문 방지). */
    @Transactional(readOnly = true)
    public Optional<Integer> findExistingOrderNo(String pgCode, String pgOrderCode) {
        if (pgCode == null || pgOrderCode == null || pgOrderCode.isBlank()) return Optional.empty();
        return orderPayService.findOrderNoByPgOrder(pgCode, pgOrderCode);
    }

    /**
     * 이미 저장된 주문의 가상계좌 입금 안내(은행·계좌·금액·마감·입금자명). 가상계좌가 아니거나 계좌 정보가 없으면 null.
     * 재전송으로 기존 주문을 돌려줄 때 안내를 다시 보여 주려고 쓴다.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> findVbankInfo(int orderNo) {
        OrderPay p = orderPayService.select(Map.of("orderNo", orderNo));
        if (p == null || p.getReciptAccnt() == null || p.getReciptAccnt().isBlank()) return null;
        return vbankInfo(p.getReciptBank(), p.getReciptAccnt(), p.getPayAmt() == null ? null : p.getPayAmt().longValue(),
                p.getPayExpectDate(), p.getPayExpectName(), null);
    }

    /** 응답에 싣는 가상계좌 입금 안내. 값이 없는 항목은 빈 문자열이 아니라 생략한다. */
    public static Map<String, Object> vbankInfo(String bank, String account, Long amount, String dueAt, String expectName, String holder) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("bank", bank);
        m.put("account", account);
        m.put("amount", amount);
        if (dueAt != null) m.put("dueAt", dueAt);
        if (expectName != null) m.put("expectName", expectName);
        if (holder != null) m.put("accountHolder", holder);
        return m;
    }

    /** 토스 승인 응답(Map)을 받는 기존 시그니처 — PG 공통 결과로 바꿔 위임한다. */
    public int checkout(CheckoutRequest req, Map<String, Object> tossResp) throws Exception {
        PaymentResult converted = TossGateway.toResult(tossResp);
        if (converted.getTradeNo() == null) converted.setTradeNo(req.getPaymentKey());
        if (converted.getOrderCode() == null) converted.setOrderCode(req.getOrderId());
        return checkout(req, converted);
    }

    public int checkout(CheckoutRequest req, PaymentResult result) throws Exception {
        String now = LocalDateTime.now().format(DATETIME_FMT);

        // PG 가 실제로 승인한 금액만 신뢰한다 — 클라이언트가 보낸 totalAmt는 참고값일 뿐,
        // 서버가 검증한 값과 다르면 결제/주문 저장을 거부한다.
        Long confirmedAmount = result.getAmount();
        if (confirmedAmount == null || req.getTotalAmt() == null
                || confirmedAmount != req.getTotalAmt().longValue()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");
        }

        // 방어선: 주문 라인 합계가 실제 승인된 금액을 넘을 수 없다(배송비가 있으면 승인액이 더 크다).
        long linesTotal = 0;
        for (CheckoutProdItem item : req.getOrderProds()) {
            long unit = (long) (item.getSalePrice() != null ? item.getSalePrice() : 0)
                    + (item.getOptionPrice() != null ? item.getOptionPrice() : 0);
            linesTotal += unit * (item.getProdQty() != null ? item.getProdQty() : 1);
        }
        if (linesTotal > confirmedAmount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 주문 금액보다 작습니다.");
        }

        // PG 결제 상태가 완료(DONE)일 때만 "지불완료"로 기록한다.
        // 가상계좌는 입금 전까지 WAITING_FOR_DEPOSIT 상태이므로 아직 결제완료로 간주하면 안 된다.
        boolean paid = result.paid();
        String orderState = paid ? "지불완료" : "주문접수";

        // 비회원 주문(CustId=0)은 로그인 계정이 없어 registId/registName이 빈 값으로 올 수 있다.
        // RegistId/RegistName은 NOT NULL 컬럼이라 빈 문자열이어도 저장 자체는 되지만, 관리자 화면에서
        // 식별 가능하도록 의미 있는 기본값을 채워 넣는다.
        boolean isGuest = req.getCustId() == null || req.getCustId() == 0;
        String registId = (req.getRegistId() == null || req.getRegistId().isBlank())
                ? (isGuest ? "GUEST" : req.getRegistId()) : req.getRegistId();
        String registName = (req.getRegistName() == null || req.getRegistName().isBlank())
                ? (isGuest ? req.getOrderName() : req.getRegistName()) : req.getRegistName();

        // 1. 주문번호 채번 (tOrdOrderNoSeq)
        OrderNoSeq seq = new OrderNoSeq();
        orderNoSeqMapper.insert(seq);
        int orderNo = seq.getSeq();

        // 2. 주문 헤더 저장 (tOrdOrder)
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setCustId(isGuest ? 0 : req.getCustId());
        order.setLoginId(registId);
        order.setOrderName(req.getOrderName());
        order.setOrderEmail(req.getOrderEmail());
        order.setOrderMobileNo(req.getOrderMobileNo());
        order.setRecverName(req.getRecverName());
        order.setRecverMobileNo(req.getRecverMobileNo());
        order.setRecverAddr1(req.getRecverAddr());
        order.setMoneyUnit("KRW");
        order.setDeliMemo(req.getDeliMemo());
        order.setOrderState(orderState);
        order.setChnlCode(shopChnlCode);
        order.setRegistId(registId);
        order.setRegistName(registName);
        if (paid) order.setPayCmpletDate(now);
        orderService.insert(order);

        // 3. 주문 상품 라인 저장 (tOrdOrderProd)
        int orderProdNo = 1;
        for (CheckoutProdItem item : req.getOrderProds()) {
            int qty   = item.getProdQty() != null ? item.getProdQty() : 1;
            int price = item.getSalePrice() != null ? item.getSalePrice() : 0;       // 상품 판매가(단가)
            int opt   = item.getOptionPrice() != null ? item.getOptionPrice() : 0;   // 옵션 추가금(단가, NvPrice)
            int list  = item.getListPrice() != null ? item.getListPrice() : price;   // 정가(단가)

            OrderProd op = new OrderProd();
            op.setOrderNo(orderNo);
            op.setOrderProdNo(orderProdNo++);
            op.setShopCode(item.getShopCode());
            op.setProdCode(item.getProdCode());
            op.setProdName(item.getProdName());
            op.setItemCode(item.getItemCode());
            op.setItemName(item.getItemName());
            op.setProdQty(qty);
            // 주문 라인 금액은 모든 채널에서 "수량이 곱해진 라인 합계"다(주문관리·마진·정산이 그대로 합산한다).
            // 주문관리 정의: 옵션가 = NvPrice, 판매가 = SalePrice + NvPrice.
            op.setListPrice(list * qty);
            op.setSalePrice(price * qty);
            op.setNvPrice(opt * qty);
            op.setReciptAmt((price + opt) * qty);
            op.setDeliFeeAmt(0);
            op.setOrderState(orderState);
            op.setState(1);
            op.setRegistId(registId);
            op.setRegistName(registName);
            orderProdService.insert(op);
        }

        // 3-1. 재고 차감 — 주문 저장과 같은 트랜잭션. 확인+차감이 한 문장이라 동시 주문이 재고를 넘겨 팔 수 없고, 부족하면 409 로
        //      주문 저장 전체가 롤백된다(컨트롤러가 이미 승인된 결제를 자동 취소한다). 데드락을 피하려고 상품코드 순으로 처리한다.
        //      라인은 위에서 1번부터 순서대로 채번했으므로 같은 순서로 번호를 다시 붙인다.
        List<Object[]> stockLines = new ArrayList<>();   // {상품코드, 라인번호, 수량}
        int lineNo = 1;
        for (CheckoutProdItem item : req.getOrderProds()) {
            stockLines.add(new Object[] {item.getProdCode(), lineNo++, item.getProdQty() != null ? item.getProdQty() : 1});
        }
        stockLines.sort(Comparator.comparing(a -> String.valueOf(a[0])));
        for (Object[] l : stockLines) {
            stockLedgerService.deductForOrder(orderNo, (Integer) l[1], (String) l[0], (Integer) l[2], registId);
        }

        // 4. 결제 정보 저장 (tOrdOrderPay)
        OrderPay pay = new OrderPay();
        pay.setOrderNo(orderNo);
        // PayNo는 전역 auto_increment이지만, 관리자 조회 쿼리(payJoin)가 OPay.PayNo = 1을
        // 주문의 대표 결제건으로 취급하므로 신규 주문의 최초 결제는 명시적으로 1을 채번한다.
        pay.setPayNo(1);
        pay.setPayType(PAY_TYPE_CODE.getOrDefault(req.getPayMethod(), req.getPayMethod()));
        pay.setPayDate(now);
        pay.setPayAmt(confirmedAmount.intValue());
        pay.setCardComp(result.getCardComp());
        pay.setApprovNo(result.getApproveNo());
        pay.setApprovDate(result.getApprovedAt());
        pay.setReciptBank(result.getReciptBank());
        pay.setReciptName(req.getOrderName());
        if (result.getAccount() != null) {
            // 가상계좌 — 계좌와 입금 예정 정보(금액·마감·입금자명)를 기록한다. ReciptAccnt 는 모델의 reciptNo 로 넘기는 기존 관례.
            pay.setReciptNo(result.getAccount());
            pay.setPayExpectAmt(java.math.BigDecimal.valueOf(confirmedAmount));
            pay.setPayExpectDate(result.getDueAt());
            pay.setPayExpectName(result.getExpectName());
        }
        pay.setPgCode(result.getPgCode());
        pay.setPgOrderNo(result.getOrderCode());
        pay.setPgTradeNo(result.getTradeNo());
        pay.setRegistId(registId);
        pay.setRegistName(registName);
        orderPayService.insert(pay);

        // 5. PG 결제 승인 원본 로그 저장 — 토스는 tOrdOrderPayLogToss (다른 PG 는 해당 PG 의 로그 테이블)
        if (TossGateway.CODE.equals(result.getPgCode()) && result.getRaw() != null) {
            orderPayLogTossMapper.insert(OrderPayLogToss.from(orderNo, result.getRaw(), registId, registName));
        }

        return orderNo;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return (o instanceof Map) ? (Map<String, Object>) o : null;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long numToLong(Object o) {
        return (o instanceof Number) ? ((Number) o).longValue() : null;
    }

}
