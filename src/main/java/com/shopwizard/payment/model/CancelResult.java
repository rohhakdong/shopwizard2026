package com.shopwizard.payment.model;

import lombok.Data;

import java.util.Map;

/** 결제 취소 결과. 실패는 예외로 던지므로 이 객체는 성공한 취소만 나타낸다. */
@Data
public class CancelResult {
    private String pgCode;
    private String tradeNo;
    /** 이번에 취소한 금액(전체취소면 취소 가능했던 전액, PG 가 알려주지 않으면 null) */
    private Long canceledAmount;
    /** PG 응답 원본 — PG 별 결제 로그에 저장한다 */
    private Map<String, Object> raw;
}
