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
}
