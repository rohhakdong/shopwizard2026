package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class CustGroup {
    private String custGroupCode;
    private String custGroupName;
    private String custGroupDesc;
    private Double custDiscntRate;
    private Double custSaveRate;
    private String custDiscntStartDate;
    private String custDiscntEndDate;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
