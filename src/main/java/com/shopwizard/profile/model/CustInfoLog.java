package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class CustInfoLog {
    private Integer logId;
    private Integer custId;
    private String menuName;
    private String execCntnts;
    private String svcName;
    private String daoName;
    private String tableName;
    private String orderNo;
    private String mngrUid;
    private String mngrLoginId;
    private String mngrName;
    private String mngrIp;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
