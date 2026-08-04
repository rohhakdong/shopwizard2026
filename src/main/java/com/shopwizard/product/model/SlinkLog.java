package com.shopwizard.product.model;

import lombok.Data;

@Data
public class SlinkLog {
    private Integer logNo;
    private String logType;
    private String prodCode;
    private String cateCode;
    private String prodName;
    private Integer saleYn;
    private Integer orderNo;
    private Integer orderProdNo;
    private Integer orderChangeNo;
    private String deliCompName;
    private String invNo;
    private String resultType;
    private String resultMessag;
    private String callUrl;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
