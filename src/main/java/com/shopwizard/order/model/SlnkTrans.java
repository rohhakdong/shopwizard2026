package com.shopwizard.order.model;

import lombok.Data;

@Data
public class SlnkTrans {
    private Integer transId;
    private String mallCode;
    private String transType;
    private String transState;
    private String transStartDate;
    private String transEndDate;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
