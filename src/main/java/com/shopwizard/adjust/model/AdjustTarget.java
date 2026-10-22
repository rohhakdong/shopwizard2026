package com.shopwizard.adjust.model;

import lombok.Data;

/**
 * 정산 대상 한 줄 — 아직 정산자료로 만들지 않은 배송(+, tShpShipDirect 송장입력 건) 또는 반품(−, tShpReturnDirect 반품완료 건).
 * 반품 행은 수량·금액이 음수로 내려온다. 한 키(주문번호·라인·변경번호)에 배송과 반품이 같이 있을 수 있어
 * 어느 쪽인지는 {@code kind} 로 구분한다(정산자료 생성 요청에도 그대로 실어 보낸다).
 */
@Data
public class AdjustTarget {
    /** 'SHIP' | 'RETURN' */
    private String kind;
    private Integer orderNo;
    private Integer orderProdNo;
    private Integer orderChangeNo;
    private String orderName;
    private String shopCode;
    private String shopName;
    private String chnlCode;
    private String chnlName;
    private String prodCode;
    private String prodName;
    private String itemName;
    private Integer prodQty;
    private Long salePrice;
    private Long supplyPrice;
    private Long buyPrice;
    private Integer vatRate;
    private Long deliFeeAmt;
    private String invNo;
    private String orderState;
    /** 배송 건은 송장입력일, 반품 건은 반품완료일 — 정산 기간 판단 기준. */
    private String cmpletDate;
}
