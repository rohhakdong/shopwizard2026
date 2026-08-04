package com.shopwizard.authority.model;

import lombok.Data;

@Data
public class Role {
    private String roleUid;
    private String roleName;
    private String roleDesc;
    private String svcCode;
    private String svcName;
    private int supplyYn;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
