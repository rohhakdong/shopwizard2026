package com.shopwizard.order.model;

import lombok.Data;

import java.util.List;

/** 서버가 계산한 주문 견적: 라인, 상품금액 합, 배송비, 결제 총액. */
@Data
public class PricedOrder {
    private List<PricedLine> lines;
    private long itemAmt;
    private int deliAmt;
    private long totalAmt;
}
