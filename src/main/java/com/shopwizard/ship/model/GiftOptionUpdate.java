package com.shopwizard.ship.model;

import lombok.Data;

@Data
public class GiftOptionUpdate {
    private Integer updateId;
    private String startDate;
    private String endDate;
    private String resultType;
    private String resultMessag;
    private String updateStartDate;
    private String updateEndDate;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
