package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class Dept {
    private String deptCode;
    private Integer branchId;
    private String deptName;
    private String deptDesc;
    private String parentDeptCode;
    private Integer deptLevel;
    private Integer deptSeq;
    private String phoneNo;
    private String faxNo;
    private String email;
    private String accntCheckYn;
    private Integer childCount;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
