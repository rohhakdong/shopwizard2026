package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderProdApprovLine {
    private Integer orderNo;
    private Integer orderProdNo;
    private Integer approvLevel;
    private Integer approvCustId;
    private String approvCustName;
    private String approvResult;
    private String approvDate;
    private String approvMemo;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
