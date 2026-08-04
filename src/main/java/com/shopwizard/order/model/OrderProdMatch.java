package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderProdMatch {
    private String prodName;
    private String keywrd;
    private String capa1;
    private String qty1;
    private String capa2;
    private String qty2;
    private String matchProdCode;
    private String prevProdName;
    private String prevProdOption;
    private String chnlName;
    private String orderNo;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
