package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class Estimt {
    private String compCode;
    private Integer branchId;
    private String deptCode;
    private Integer accntId;
    private String accntName;
    private String startDate;
    private String endDate;
    private Integer estimtAmt;
    private Integer useAmt;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
