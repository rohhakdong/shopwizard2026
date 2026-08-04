package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class CustConcrnProdReport {
    private String prodCode;
    private String shopProdCode;
    private String prodName;
    private String prodUnit;
    private String attrValStr1;
    private String imgUrl80;
    private String brandName;
    private String modelName;
    private Integer salePrice;
    private Integer vatRate;
    private String makerName;
    private String originName;
    private String deliFeeType;
    private Integer deliFeeAmt;
    private Integer orderCount;
    private Integer orderQty;
    private Integer saleYn;
    private String lastOrderDate;
    private String registDate;
    private String changeDate;
}
