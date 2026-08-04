package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class CustApprovLine {
    private Integer custId;
    private Integer approvLevel;
    private Integer approvCustId;
    private String approvType;
    private String finalYn;
    private String custName;
    private String compName;
    private String branchName;
    private String deptName;
    private String dutyName;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
