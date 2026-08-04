package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderSoclQuot {
    private String chnlName;
    private String dealName;
    private String matchProdCode;
    private String shopCode;
    private String dealNo;
    private String prodCode;
    private String prodName;
    private String optionNo;
    private String optionName;
    private String supplyPrice;
    private String cost;
    private String orderProdName;
    private String orderProdQty;
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
    private String oldProdCode;
}
