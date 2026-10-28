package com.shopwizard.payment.model;

import lombok.Data;

/**
 * 결제 취소 요청. {@code amount} 가 null 이면 전체취소, 값이 있으면 그 금액만큼 부분취소.
 * {@code idempotencyKey} 는 같은 키로 다시 호출해도 이중 취소가 되지 않게 하는 PG(토스)가 쓰고, 멱등키가 없는 PG(KCP)는 무시한다.
 */
@Data
public class CancelRequest {
    private String tradeNo;
    private String reason;
    private Long amount;
    private String idempotencyKey;
    /** tOrdOrderPay.PayType(12자리 코드) — KCP 는 거래 조회·취소에 결제수단이 필요하다. 토스는 쓰지 않는다. */
    private String payType;
    /**
     * 이번 취소 <b>직전에 있어야 할</b> 결제 잔액(결제금액 − 이미 환불한 금액). 멱등키가 없는 PG(KCP)가 재시도 때 이중 취소를
     * 막는 데 쓴다 — PG 잔액이 (직전 잔액 − 이번 금액)이면 이미 취소된 것으로 보고 다시 호출하지 않고, 직전 잔액과도 다르면
     * 기록이 어긋난 것이라 취소하지 않고 실패시킨다. 알 수 없으면 null.
     */
    private Long remainingBefore;

    public boolean partial() {
        return amount != null;
    }
}
