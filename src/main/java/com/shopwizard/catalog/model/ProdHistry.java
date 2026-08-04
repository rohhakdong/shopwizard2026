package com.shopwizard.catalog.model;

import lombok.Data;

@Data
public class ProdHistry {
    private String prodCode;
    private Integer changeNo;
    private String histryType;
    private String cateCode;
    private String prodName;
    private String attrName1; private String attrName2; private String attrName3; private String attrName4;
    private String attrValStr1; private String attrValStr2; private String attrValStr3; private String attrValStr4;
    private Integer saleYn;
    private Integer leadTime;
    private String leadTimeReason;
    private Integer supplyQty;
    private Integer listPrice;
    private Integer prevPrice;
    private Integer salePrice;
    private Integer discntPrice;
    private String discntStartDate;
    private String discntEndDate;
    private Integer supplyPrice;
    private Integer discntSupplyPrice;
    private Integer buyPrice;
    private Integer vatRate;
    private Integer vatAmt;
    private Integer originId;
    private Integer makerId;
    private Integer brandId;
    private String deliFeeCode;
    private String remark;
    private Integer state;
    private String registDate; private String registId; private String registName;
    private String changeDate; private String changeId; private String changeName;
}
