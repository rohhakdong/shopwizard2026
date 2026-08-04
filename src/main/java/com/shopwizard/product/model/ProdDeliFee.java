package com.shopwizard.product.model;

import lombok.Data;

@Data
public class ProdDeliFee {
    private Integer deliFeeId;
    private String deliFeeName;
    private String deliFeeDesc;
    private Integer deliFee1;
    private Integer limitAmt1;
    private Integer deliFee2;
    private Integer limitAmt2;
    private Integer deliFee3;
    private Integer limitAmt3;
    private String deliFeeType;
    private Integer vatRate;
    private String svcCode;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
