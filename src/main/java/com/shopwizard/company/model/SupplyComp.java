package com.shopwizard.company.model;

import lombok.Data;

@Data
public class SupplyComp {
    private String supplyCode;
    private String supplyType;
    private String supplyName;
    private String applyStartDate;
    private String applyEndDate;
    private String mngrName;
    private String phoneNo;
    private String mobileNo;
    private String faxNo;
    private String email;
    private String zipcode;
    private String addr1;
    private String addr2;
    private String compCode;
    private String approvDate;
    private String svcCode;
    private Comp comp;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
