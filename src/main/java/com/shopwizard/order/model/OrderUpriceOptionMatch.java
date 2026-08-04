package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderUpriceOptionMatch {
    private String orderNo;
    private String orderSeq;
    private String keywrd;
    private String capa;
    private String qty;
    private String matchUpriceCode;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
