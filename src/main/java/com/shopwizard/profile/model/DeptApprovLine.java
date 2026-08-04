package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class DeptApprovLine {
    private Integer deptId;
    private Integer approvLevel;
    private Integer approvCustId;
    private String approvType;
    private Integer finalYn;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
