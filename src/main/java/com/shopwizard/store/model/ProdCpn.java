package com.shopwizard.store.model;

import lombok.Data;

@Data
public class ProdCpn {
    private Integer cpnId;
    private String cpnName;
    private String cpnDesc;
    private String cpnImg;
    private String cpnType;
    private Integer cpnAmt;
    private Integer limitAmt;
    private String cpnStartDate;
    private String cpnEndDate;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
