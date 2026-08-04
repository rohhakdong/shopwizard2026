package com.shopwizard.adjust.model;

import lombok.Data;

@Data
public class Adjust {
    private Integer adjustNo;
    private String supplyCode;
    private String supplyName;
    private String shopCode;
    private String shopName;
    private String adjustStartDate;
    private String adjustEndDate;
    private String adjustMonth;
    private String payDate;
    private String reciptBank;
    private String reciptAccnt;
    private String reciptDepost;
    private String chnlCode;
    private String chnlName;
    private String compCode;
    private String compName;
    private Integer branchId;
    private String branchCode;
    private String branchName;
    private Integer vatRate;
    private String adjustPeriodCode;
    private String taxinvCheckDate;
    private String taxinvSect;
    private String taxinvMemo;
    private String taxinvImg;
    private Integer saleAmt;
    private Integer buyAmt;
    private Integer prodMargin;
    private Integer deliFeeAmt;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
