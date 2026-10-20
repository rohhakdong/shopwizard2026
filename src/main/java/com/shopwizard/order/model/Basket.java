package com.shopwizard.order.model;

import lombok.Data;

/**
 * shopion.tOrdBasket 한 행 + 화면 표시용 조인 값.
 * 테이블에는 상품·수량 참조만 저장하고, 상품명/이미지/가격은 조회 시 tPrdProd 에서 채운다(읽기 전용).
 */
@Data
public class Basket {
    private Integer basketNo;
    private String custId;
    private String sessonId;
    private String prodCode;
    private String itemCode;
    private Integer prodQty;
    private String directOrderYn;
    private String chnlCode;
    private String storeCode;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
    // join fields (읽기 전용 — insert 에서는 쓰지 않는다)
    private String prodName;
    private String prodImg;
    private String itemName;
    private String shopCode;
    private Integer listPrice;
    private Integer salePrice;
    private Integer supplyPrice;
    private Integer deliFeeAmt;
}
