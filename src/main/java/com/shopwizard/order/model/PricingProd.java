package com.shopwizard.order.model;

import lombok.Data;

import java.math.BigDecimal;

/** 체크아웃 가격 계산용 상품 행(shopion.tPrdProd 읽기 전용 투영). */
@Data
public class PricingProd {
    private String prodCode;
    private String prodName;
    private String shopCode;
    private Integer saleYn;
    private Integer state;
    /** 상품 재고. null 이면 재고 미관리(무제한). */
    private Integer supplyQty;
    private BigDecimal listPrice;
    private BigDecimal salePrice;
    private String deliFeeType;
    private BigDecimal deliFeeAmt;
}
