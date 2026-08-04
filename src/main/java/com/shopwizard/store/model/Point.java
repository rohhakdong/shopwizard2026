package com.shopwizard.store.model;

import lombok.Data;

@Data
public class Point {
    private Integer pointId;
    private String pointName;
    private String pointDesc;
    private String pointType;
    private Integer pointAmt;
    private Integer limitAmt;
    private String validStartDate;
    private String validEndDate;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
