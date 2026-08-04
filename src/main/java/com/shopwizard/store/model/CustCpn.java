package com.shopwizard.store.model;

import lombok.Data;

@Data
public class CustCpn {
    private Integer cpnId;
    private String cpnName;
    private String cpnDesc;
    private String cpnImg;
    private String discntType;
    private Integer discntAmt;
    private Integer limitAmt;
    private String discntStartDate;
    private String discntEndDate;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
