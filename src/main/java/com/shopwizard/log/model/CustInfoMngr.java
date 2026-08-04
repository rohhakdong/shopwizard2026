package com.shopwizard.log.model;

import lombok.Data;

@Data
public class CustInfoMngr {
    private Integer logId;
    private Integer custId;
    private String menuName;
    private String svcName;
    private String daoName;
    private String tableName;
    private Integer orderNo;
    private String mngrIpAddr;
    private String mngrUid;
    private String execCntnts;
    private String execResult;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
}
