package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderDeliFee {
    private Integer deliFeeId;
    private Integer orderNo;
    private String deliFeeName;
    private String deliFeeType;
    private Integer deliFeeAmt;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
