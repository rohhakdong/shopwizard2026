package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderCashRecipt {
    private Integer orderNo;
    private String reciptType;
    private String reciptNo;
    private String reciptDate;
    private Integer reciptAmt;
    private String custName;
    private String custEmail;
    private String pgResultCode;
    private String pgResultMsg;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
    // join fields
    private String orderName;
    private String orderState;
    private String loginId;
}
