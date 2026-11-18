package com.shopwizard.order.model;

/** 수기 주문 등록 결과 — 새 주문번호, 상품 줄 수, 주문 합계(판매가+옵션가, 수량 반영). */
public record ManualOrderResult(int orderNo, int lineCount, long totalAmt) {
}
