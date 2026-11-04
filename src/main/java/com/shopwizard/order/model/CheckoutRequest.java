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
    // 결제 PG (비어 있으면 payment.default-pg) 와 PG 별 인증 결과(토스: paymentKey / KCP: encData, encInfo, tranCd …)
    private String pgCode;
    private java.util.Map<String, String> payload;

    // 토스 결제 승인용 (하위 호환 — payload.paymentKey 와 같은 값)
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
    /** 예전 한 칸 주소(하위 호환) — 구조화 주소(recverAddr1)가 없을 때만 기본주소로 쓴다. */
    private String recverAddr;
    private String recverZipcode;
    private String recverAddr1;
    private String recverAddr2;
    /** 회원이 "배송지 목록에 저장"을 켠 경우 — 주문이 성공한 뒤에만(실패해도 주문은 그대로) 저장한다. */
    private boolean saveDeliAddr;
    private String deliMemo;
    private String payMethod;   // card | trans | vbank
    private Integer totalAmt;
    private String registId;
    private String registName;

    private List<CheckoutProdItem> orderProds;

    /** 저장·검증에 쓰는 기본주소 — 구조화 주소가 오면 그 값, 아니면 예전 한 칸 주소. 앞뒤 공백은 뗀다. */
    public String effectiveAddr1() {
        if (recverAddr1 != null && !recverAddr1.isBlank()) return recverAddr1.trim();
        return recverAddr == null ? "" : recverAddr.trim();
    }

    /** 상세주소 — 없으면 빈 문자열(NULL 이면 출고지시의 주소 합치기 쿼리가 NULL 이 된다). */
    public String effectiveAddr2() {
        return recverAddr2 == null ? "" : recverAddr2.trim();
    }
}
