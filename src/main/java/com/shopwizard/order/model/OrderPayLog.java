package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderPayLog {
    private Integer logId;
    private Integer orderNo;
    private String pgCode;
    private String pgOrderNo;
    private String pgTradeNo;
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
    private String resultCode;
    private String resultMsg;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    // join fields
    private String orderName;
    private String orderState;
    private String chnlCode;
    private String chnlName;
}
