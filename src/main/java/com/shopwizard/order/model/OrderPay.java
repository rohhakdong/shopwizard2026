package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderPay {
    private Integer orderNo;
    private Integer payNo;
    private String payType;
    private String payTypeName;
    private String payDate;
    private Integer payAmt;
    private Integer payFee;
    private String cardComp;
    private String approvNo;
    private String approvDate;
    private String reciptBank;
    private String reciptName;
    private String reciptNo;
    private String reciptDate;
    private String pgCode;
    private String pgOrderNo;
    private String pgTradeNo;
    private String payCompName;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
