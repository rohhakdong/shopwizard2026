package com.shopwizard.company.model;

import lombok.Data;

@Data
public class SaleComp {
    private String saleCompCode;
    private String saleCompName;
    private String saleCompDesc;
    private String compCode;
    private String svcCode;
    private String approvDate;
    private Comp comp;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
