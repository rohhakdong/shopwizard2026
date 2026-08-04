package com.shopwizard.order.model;

import lombok.Data;

@Data
public class SlnkTransDetail {
    private Integer transId;
    private Integer transDetailNo;
    private String transData;
    private String resultState;
    private String resultMsg;
    private Integer orderNo;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
