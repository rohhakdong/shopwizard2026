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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

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

    public int checkout(CheckoutRequest req, Map<String, Object> tossResp) throws Exception {
        String now = LocalDateTime.now().format(DATETIME_FMT);

        // 토스가 실제로 승인한 금액만 신뢰한다 — 클라이언트가 보낸 totalAmt는 참고값일 뿐,
        // 서버가 검증한 값과 다르면 결제/주문 저장을 거부한다.
        Long confirmedAmount = numToLong(tossResp.get("totalAmount"));
        if (confirmedAmount == null || req.getTotalAmt() == null
                || confirmedAmount != req.getTotalAmt().longValue()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");
        }

        // 토스 결제 상태가 완료(DONE)일 때만 "지불완료"로 기록한다.
        // 가상계좌는 입금 전까지 WAITING_FOR_DEPOSIT 상태이므로 아직 결제완료로 간주하면 안 된다.
        boolean paid = "DONE".equals(str(tossResp.get("status")));
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
            int price = item.getSalePrice() != null ? item.getSalePrice() : 0;

            OrderProd op = new OrderProd();
            op.setOrderNo(orderNo);
            op.setOrderProdNo(orderProdNo++);
            op.setShopCode(item.getShopCode());
            op.setProdCode(item.getProdCode());
            op.setProdName(item.getProdName());
            op.setItemCode(item.getItemCode());
            op.setItemName(item.getItemName());
            op.setProdQty(qty);
            op.setListPrice(price);
            op.setSalePrice(price);
            op.setReciptAmt(price * qty);
            op.setDeliFeeAmt(0);
            op.setOrderState(orderState);
            op.setState(1);
            op.setRegistId(registId);
            op.setRegistName(registName);
            orderProdService.insert(op);
        }

        // 4. 결제 정보 저장 (tOrdOrderPay)
        Map<String, Object> card          = asMap(tossResp.get("card"));
        Map<String, Object> transfer      = asMap(tossResp.get("transfer"));
        Map<String, Object> virtualAccnt  = asMap(tossResp.get("virtualAccount"));

        OrderPay pay = new OrderPay();
        pay.setOrderNo(orderNo);
        // PayNo는 전역 auto_increment이지만, 관리자 조회 쿼리(payJoin)가 OPay.PayNo = 1을
        // 주문의 대표 결제건으로 취급하므로 신규 주문의 최초 결제는 명시적으로 1을 채번한다.
        pay.setPayNo(1);
        pay.setPayType(PAY_TYPE_CODE.getOrDefault(req.getPayMethod(), req.getPayMethod()));
        pay.setPayDate(now);
        pay.setPayAmt(confirmedAmount.intValue());
        pay.setCardComp(card != null ? str(card.get("company")) : null);
        pay.setApprovNo(card != null ? str(card.get("approveNo")) : null);
        pay.setApprovDate(OrderPayLogToss.toMysqlDateTime(str(tossResp.get("approvedAt"))));
        pay.setReciptBank(transfer != null ? str(transfer.get("bank"))
                : virtualAccnt != null ? str(virtualAccnt.get("bankCode")) : null);
        pay.setReciptName(req.getOrderName());
        pay.setPgCode("TOSS");
        pay.setPgOrderNo(req.getOrderId());
        pay.setPgTradeNo(req.getPaymentKey());
        pay.setRegistId(registId);
        pay.setRegistName(registName);
        orderPayService.insert(pay);

        // 5. 토스 결제 승인 원본 로그 저장 (tOrdOrderPayLogToss)
        OrderPayLogToss log = OrderPayLogToss.from(orderNo, tossResp, registId, registName);
        orderPayLogTossMapper.insert(log);

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
