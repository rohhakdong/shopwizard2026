package com.shopwizard.catalog.model;

import lombok.Data;

@Data
public class ProdItemHistry {
    private String prodCode;
    private Integer itemCode;
    private Integer changeNo;
    private String attrVal1; private String attrVal2; private String attrVal3; private String attrVal4;
    private Integer supplyQty;
    private Integer saleYn;
    private String saleStopDate;
    private String saleStartDate;
    private String saleEndDate;
    private String remark;
    private Integer state;
    private String registDate; private String registId; private String registName;
    private String changeDate; private String changeId; private String changeName;
}
