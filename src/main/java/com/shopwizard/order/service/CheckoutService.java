package com.shopwizard.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderNoSeqMapper;
import com.shopwizard.order.mapper.OrderPayLogTossMapper;
import com.shopwizard.order.mapper.OrderPayMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.CheckoutProdItem;
import com.shopwizard.order.model.CheckoutRequest;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderNoSeq;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.order.model.OrderPayLogToss;
import com.shopwizard.order.model.OrderProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
public class CheckoutService {

    /** ShopWizard 자사 쇼핑몰(shop.html) 채널코드 */
    private static final String SHOP_CHNL_CODE = "1208674775Z2";

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** tOrdOrderPay.PayType 값 (wizardn.tCodConstrVal, ConstrCode='cPayType' 코드값) */
    private static final Map<String, String> PAY_TYPE_CODE = Map.of(
            "card",  "100000000000",  // 신용카드
            "trans", "010000000000",  // 계좌이체
            "vbank", "001000000000"   // 가상계좌
    );

    private final OrderNoSeqMapper orderNoSeqMapper;
    private final OrderMapper orderMapper;
    private final OrderProdMapper orderProdMapper;
    private final OrderPayMapper orderPayMapper;
    private final OrderPayLogTossMapper orderPayLogTossMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public int checkout(CheckoutRequest req, Map<String, Object> tossResp) throws Exception {
        String now = LocalDateTime.now().format(DATETIME_FMT);

        // 1. 주문번호 채번 (tOrdOrderNoSeq)
        OrderNoSeq seq = new OrderNoSeq();
        orderNoSeqMapper.insert(seq);
        int orderNo = seq.getSeq();

        // 2. 주문 헤더 저장 (tOrdOrder)
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setCustId(req.getCustId());
        order.setLoginId(req.getRegistId());
        order.setOrderName(req.getOrderName());
        order.setOrderEmail(req.getOrderEmail());
        order.setOrderMobileNo(req.getOrderMobileNo());
        order.setRecverName(req.getRecverName());
        order.setRecverMobileNo(req.getRecverMobileNo());
        order.setRecverAddr1(req.getRecverAddr());
        order.setMoneyUnit("KRW");
        order.setDeliMemo(req.getDeliMemo());
        order.setOrderState("지불완료");
        order.setChnlCode(SHOP_CHNL_CODE);
        order.setRegistId(req.getRegistId());
        order.setRegistName(req.getRegistName());
        order.setPayCmpletDate(now);
        orderMapper.insert(order);

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
            op.setOrderState("지불완료");
            op.setState(1);
            op.setRegistId(req.getRegistId());
            op.setRegistName(req.getRegistName());
            orderProdMapper.insert(op);
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
        pay.setPayAmt(req.getTotalAmt());
        pay.setCardComp(card != null ? str(card.get("company")) : null);
        pay.setApprovNo(card != null ? str(card.get("approveNo")) : null);
        pay.setApprovDate(toMysqlDateTime(str(tossResp.get("approvedAt"))));
        pay.setReciptBank(transfer != null ? str(transfer.get("bank"))
                : virtualAccnt != null ? str(virtualAccnt.get("bankCode")) : null);
        pay.setReciptName(req.getOrderName());
        pay.setPgCode("TOSS");
        pay.setPgOrderNo(req.getOrderId());
        pay.setPgTradeNo(req.getPaymentKey());
        pay.setRegistId(req.getRegistId());
        pay.setRegistName(req.getRegistName());
        orderPayMapper.insert(pay);

        // 5. 토스 결제 승인 원본 로그 저장 (tOrdOrderPayLogToss)
        Map<String, Object> receipt = asMap(tossResp.get("receipt"));

        OrderPayLogToss log = new OrderPayLogToss();
        log.setOrderNo(orderNo);
        log.setPaymentKey(req.getPaymentKey());
        log.setOrderId(req.getOrderId());
        log.setMethod(str(tossResp.get("method")));
        log.setStatus(str(tossResp.get("status")));
        log.setTotalAmount(numToLong(tossResp.get("totalAmount")));
        log.setBalanceAmount(numToLong(tossResp.get("balanceAmount")));
        log.setSuppliedAmount(numToLong(tossResp.get("suppliedAmount")));
        log.setVat(numToLong(tossResp.get("vat")));
        log.setCurrency(str(tossResp.get("currency")));
        log.setRequestedAt(str(tossResp.get("requestedAt")));
        log.setApprovedAt(str(tossResp.get("approvedAt")));
        log.setCardCompany(card != null ? str(card.get("company")) : null);
        log.setCardNumber(card != null ? str(card.get("number")) : null);
        log.setCardApproveNo(card != null ? str(card.get("approveNo")) : null);
        log.setCardInstallmentPlanMonths(card != null ? intVal(card.get("installmentPlanMonths")) : null);
        log.setReceiptUrl(receipt != null ? str(receipt.get("url")) : null);
        log.setSuccess(true);
        log.setRawResponse(objectMapper.writeValueAsString(tossResp));
        log.setRegistId(req.getRegistId());
        log.setRegistName(req.getRegistName());
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

    private static Integer intVal(Object o) {
        return (o instanceof Number) ? ((Number) o).intValue() : null;
    }

    /** 토스 응답의 ISO-8601 시각(예: 2026-08-05T16:50:55+09:00)을 MySQL DATETIME 형식으로 변환 */
    private static String toMysqlDateTime(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return java.time.OffsetDateTime.parse(iso).format(DATETIME_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
