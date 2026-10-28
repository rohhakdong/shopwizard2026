package com.shopwizard.payment.service;

import com.shopwizard.order.model.OrderPayLogToss;
import com.shopwizard.payment.model.ApproveRequest;
import com.shopwizard.payment.model.CancelRequest;
import com.shopwizard.payment.model.CancelResult;
import com.shopwizard.payment.model.PayMethodOption;
import com.shopwizard.payment.model.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 토스페이먼츠. 기존 {@link TossPaymentService} 호출을 PG 공통 인터페이스로 감싼다(동작은 그대로). */
@Component
@RequiredArgsConstructor
public class TossGateway implements PaymentGateway {

    public static final String CODE = "TOSS";

    private final TossPaymentService toss;

    /** 결제창(JS SDK)에 쓰는 공개 클라이언트 키 — 예전에는 shop.html 에 박혀 있었다. */
    @Value("${toss.client-key:}")
    private String clientKey;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String label() {
        return "토스페이먼츠";
    }

    @Override
    public List<PayMethodOption> methods() {
        return List.of(
                new PayMethodOption("card", "신용카드"),
                new PayMethodOption("trans", "계좌이체"),
                new PayMethodOption("vbank", "가상계좌"));
    }

    @Override
    public Map<String, Object> clientConfig() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clientKey", clientKey);
        return m;
    }

    @Override
    public PaymentResult approve(ApproveRequest req) throws Exception {
        String paymentKey = req.payload("paymentKey");
        if (paymentKey == null || paymentKey.isBlank() || req.getOrderCode() == null || req.getOrderCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 정보가 올바르지 않습니다.");
        }
        Map<String, Object> resp = toss.confirm(paymentKey, req.getOrderCode(), req.getAmount());
        return toResult(resp);
    }

    @Override
    public CancelResult cancel(CancelRequest req) throws Exception {
        Map<String, Object> resp = req.partial()
                ? toss.cancelPartial(req.getTradeNo(), req.getReason(), req.getAmount(), req.getIdempotencyKey())
                : toss.cancel(req.getTradeNo(), req.getReason());
        CancelResult r = new CancelResult();
        r.setPgCode(CODE);
        r.setTradeNo(req.getTradeNo());
        r.setCanceledAmount(req.getAmount());
        r.setRaw(resp);
        return r;
    }

    /** 토스 결제 응답(승인/조회) → PG 공통 결과. */
    public static PaymentResult toResult(Map<String, Object> resp) {
        Map<String, Object> card = asMap(resp.get("card"));
        Map<String, Object> transfer = asMap(resp.get("transfer"));
        Map<String, Object> virtualAccount = asMap(resp.get("virtualAccount"));

        PaymentResult r = new PaymentResult();
        r.setPgCode(CODE);
        r.setTradeNo(str(resp.get("paymentKey")));
        r.setOrderCode(str(resp.get("orderId")));
        r.setAmount(resp.get("totalAmount") instanceof Number n ? n.longValue() : null);
        r.setStatus(str(resp.get("status")));
        r.setMethod(methodOf(str(resp.get("method"))));
        r.setCardComp(card != null ? str(card.get("company")) : null);
        r.setApproveNo(card != null ? str(card.get("approveNo")) : null);
        r.setApprovedAt(OrderPayLogToss.toMysqlDateTime(str(resp.get("approvedAt"))));
        r.setReciptBank(transfer != null ? str(transfer.get("bank"))
                : virtualAccount != null ? str(virtualAccount.get("bankCode")) : null);
        r.setRaw(resp);
        return r;
    }

    private static String methodOf(String tossMethod) {
        if (tossMethod == null) return null;
        return switch (tossMethod) {
            case "카드" -> "CARD";
            case "계좌이체" -> "TRANS";
            case "가상계좌" -> "VBANK";
            case "간편결제" -> "EASY";
            default -> tossMethod;
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return (o instanceof Map) ? (Map<String, Object>) o : null;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
