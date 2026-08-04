package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class DeptStore {
    private Integer deptId;
    private String storeCode;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
