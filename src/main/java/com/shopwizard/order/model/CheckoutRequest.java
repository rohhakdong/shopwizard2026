package com.shopwizard.order.model;

import lombok.Data;

import java.util.List;

/**
 * shop.html 결제완료 콜백(POST /order/checkout) 요청 바디.
 * 토스 결제 승인 + 주문 저장(tOrdOrder/tOrdOrderProd/tOrdOrderPay/tOrdOrderPayLogToss)을
 * 하나의 트랜잭션으로 처리하기 위한 DTO.
 */
@Data
public class CheckoutRequest {
    // 토스 결제 승인용
    private String paymentKey;
    private String orderId;     // 토스 orderId (예: SW-...)
    private Long amount;

    // 주문자 / 배송지 정보
    private Integer custId;
    private String orderName;
    private String orderMobileNo;
    private String orderEmail;
    private String recverName;
    private String recverMobileNo;
    private String recverAddr;
    private String deliMemo;
    private String payMethod;   // card | trans | vbank
    private Integer totalAmt;
    private String registId;
    private String registName;

    private List<CheckoutProdItem> orderProds;
}
