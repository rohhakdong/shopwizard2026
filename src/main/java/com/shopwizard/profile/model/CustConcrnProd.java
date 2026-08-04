package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class CustConcrnProd {
    private Integer custId;
    private String custName;
    private String prodCode;
    private String chnlCode;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
