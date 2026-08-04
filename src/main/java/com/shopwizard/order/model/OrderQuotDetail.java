package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderQuotDetail {
    private String quotNo;
    private String quotSeq;
    private String keywrd;
    private String capa;
    private String matchUpriceCode;
    private String orderProdName;
    private String orderProdQty;
    private String supplyPrice;
    private String cost;
    private Integer giftYn;
    private String resultType;
    private String resultMessag;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
