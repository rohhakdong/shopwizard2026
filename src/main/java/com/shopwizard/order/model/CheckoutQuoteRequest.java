package com.shopwizard.order.model;

import lombok.Data;

import java.util.List;

/** POST /order/checkout/quote 요청 바디 — 상품코드·옵션코드·수량만 의미가 있다(가격은 서버가 정한다). */
@Data
public class CheckoutQuoteRequest {
    private List<CheckoutProdItem> orderProds;
}
