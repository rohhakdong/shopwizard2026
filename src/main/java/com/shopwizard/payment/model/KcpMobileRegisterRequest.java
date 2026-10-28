package com.shopwizard.payment.model;

import com.shopwizard.order.model.CheckoutProdItem;
import lombok.Data;

import java.util.List;

/**
 * POST /payment/kcp/register 요청 — 모바일 결제창을 열기 위한 거래등록.
 * 금액은 받지 않는다: 상품 코드·수량으로 서버가 직접 계산한다(클라이언트 금액 불신).
 */
@Data
public class KcpMobileRegisterRequest {
    /** 가맹점 주문번호(승인 때 ordr_no 로 다시 쓰인다). */
    private String orderId;
    /** 주문서 결제수단 — card | trans */
    private String payMethod;
    private String orderName;
    private List<CheckoutProdItem> orderProds;
}
