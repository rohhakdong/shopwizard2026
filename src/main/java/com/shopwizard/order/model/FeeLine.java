package com.shopwizard.order.model;

import lombok.Data;

/** 배송비·판촉비 규칙 적용 대상 주문 라인(지불완료) — 규칙 조건 판정과 현재 수수료 값만 담는다. */
@Data
public class FeeLine {
    private Integer orderNo;
    private Integer orderProdNo;
    private String chnlCode;
    private String chnlName;
    private String prodName;
    private String itemName;
    private String dealName;
    private Integer prodQty;
    /** 라인 합계 판매가(수량이 곱해진 값). */
    private Integer salePrice;
    private Integer supplyPrice;
    /** 현재 배송형태 — 무료/선불/착불, 샵링커 수집분은 0/1/2 로 들어 있을 수 있다. */
    private String deliFeeType;
    private Integer deliFeeAmt;
    private Integer promotFeeAmt;
    private String recverAddr;
    private String recverName;
}
