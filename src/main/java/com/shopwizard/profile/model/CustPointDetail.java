package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class CustPointDetail {
    private Integer custId;
    private String custName;
    private Integer pointNo;
    private Integer pointAmt;
    private String saveReasonCode;
    private Integer pointId;
    private Integer orderNo;
    private Integer orderProdNo;
    private String validStartDate;
    private String validEndDate;
    private String acquisDate;
    private String saveReason;
    private String chnlCode;
    private String pointName;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
