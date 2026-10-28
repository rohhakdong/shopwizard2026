package com.shopwizard.payment.model;

/** 주문서에 보여줄 결제수단 하나. {@code value} 는 CheckoutRequest.payMethod 로 서버에 돌아온다. */
public record PayMethodOption(String value, String label) {
}
