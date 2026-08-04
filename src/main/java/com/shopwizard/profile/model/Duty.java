package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class Duty {
    private Integer dutyId;
    private String compCode;
    private String dutyName;
    private String dutyDesc;
    private Integer dutySeq;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
