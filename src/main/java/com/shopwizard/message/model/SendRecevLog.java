package com.shopwizard.message.model;

import lombok.Data;

@Data
public class SendRecevLog {
    private Integer logId;
    private String docType;
    private String sendTermnlCode;
    private String recevTermnlCode;
    private String tableName;
    private String val;
    private String transType;
    private String transResultType;
    private String transResultMessag;
    private String fileLoc;
    private String transDate;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
