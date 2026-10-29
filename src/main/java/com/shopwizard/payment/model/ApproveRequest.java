package com.shopwizard.payment.model;

import lombok.Data;

import java.util.Map;

/**
 * 결제 승인 요청. 금액({@code amount})은 서버가 상품 테이블로 계산한 합계이고, PG 는 이 값으로 승인 금액을 검증한다
 * (토스는 승인 API 의 amount, KCP 는 ordr_mony).
 */
@Data
public class ApproveRequest {
    /** 가맹점 주문번호(토스 orderId / KCP ordr_idxx) */
    private String orderCode;
    /** 서버가 계산한 결제 금액 */
    private long amount;
    /** 고객이 선택한 결제수단(card | trans | vbank | easy …) */
    private String payMethod;
    /** 주문자명 — 가상계좌 입금자명 등에 쓴다. */
    private String buyerName;
    /** 상품명(PG 에 표시) */
    private String goodName;
    /** PG 별 인증 결과 — 토스: paymentKey / KCP: encData, encInfo, tranCd … (KCP 가상계좌는 bankCode) */
    private Map<String, String> payload = Map.of();

    public String payload(String key) {
        return payload == null ? null : payload.get(key);
    }
}
