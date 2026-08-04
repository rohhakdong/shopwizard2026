package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderUpriceMatch {
    private String chnlName;
    private String eventName;
    private String prodName;
    private String itemName;
    private String orderNo;
    private String prevProdName;
    private String prevProdOption;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
