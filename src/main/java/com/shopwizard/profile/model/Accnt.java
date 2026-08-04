package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class Accnt {
    private Integer accntId;
    private String compCode;
    private String accntCode;
    private String accntName;
    private String accntDesc;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
