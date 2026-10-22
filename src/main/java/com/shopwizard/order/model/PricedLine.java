package com.shopwizard.order.model;

import lombok.Data;

/** 서버가 상품 테이블 기준으로 계산한 주문 라인. 단가 = 판매가 + 옵션 추가금. */
@Data
public class PricedLine {
    private String prodCode;
    private String prodName;
    private String shopCode;
    private String itemCode;
    private String itemName;
    private int prodQty;
    private int listPrice;
    /** 상품 판매가 */
    private int salePrice;
    /** 옵션 추가금 */
    private int optionPrice;
    /** 판매가 + 옵션 추가금 */
    private int unitPrice;
    /** 단가 × 수량 */
    private long lineAmt;
}
