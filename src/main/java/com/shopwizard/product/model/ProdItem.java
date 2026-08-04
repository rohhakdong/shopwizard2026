package com.shopwizard.product.model;

import lombok.Data;

@Data
public class ProdItem {
    private String prodCode;
    private String itemCode;
    private String attrVal1;
    private String attrVal2;
    private String attrVal3;
    private String attrVal4;
    private Integer supplyQty;
    private Integer leadTime;
    private String leadTimeReason;
    private Integer saleYn;
    private String saleStopDate;
    private String reservReason;
    private String reservStartDate;
    private String reservEndDate;
    private String reservSupplyDate;
    private Integer reservSupplyQty;
    private Integer salePrice;
    private Integer supplyPrice;
    private Integer buyPrice;
    private Integer optionPrice;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
