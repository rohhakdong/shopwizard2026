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

    public boolean partial() {
        return amount != null;
    }
}
