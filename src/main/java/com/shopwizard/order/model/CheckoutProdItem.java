package com.shopwizard.order.model;

import lombok.Data;

/** shop.html 체크아웃 요청의 주문 상품 라인 항목 */
@Data
public class CheckoutProdItem {
    private String prodCode;
    private String prodName;
    private String itemCode;
    private String itemName;
    private Integer prodQty;
    private Integer salePrice;
    private String shopCode;
    // 아래 두 필드는 클라이언트가 보내도 무시된다 — CheckoutController 가 서버 계산값(CheckoutPricingService)으로 채운다.
    private Integer listPrice;
    private Integer optionPrice;
}
