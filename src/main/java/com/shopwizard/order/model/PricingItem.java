package com.shopwizard.order.model;

import lombok.Data;

import java.math.BigDecimal;

/** 체크아웃 가격 계산용 옵션 행(shopion.tPrdProdItem 읽기 전용 투영). */
@Data
public class PricingItem {
    private String itemCode;
    /** AttrVal1~4 를 " / " 로 이은 표시용 이름 (옵션이 없으면 null). */
    private String itemName;
    private Integer saleYn;
    private Integer supplyQty;
    private BigDecimal optionPrice;
}
