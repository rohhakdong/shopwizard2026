package com.shopwizard.adjust.model;

import lombok.Data;

@Data
public class AdjustPeriod {
    private String adjustPeriodCode;
    private String adjustPeriodName;
    private String adjustPeriodDesc;
    private String defaltYn;
    private String adjustDateType;
    private String weekDateType;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
