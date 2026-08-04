package com.shopwizard.operate.model;

import lombok.Data;

@Data
public class QuotRqst {
    private Integer quotRqstNo;
    private String rqstProdName;
    private String rqstProdAttr;
    private String cateName;
    private String makerName;
    private String modelName;
    private Integer rqstQty;
    private String prodUnit;
    private String custOrderNo;
    private String rqstMemo;
    private String atchFile;
    private String quotState;
    private String quotDate;
    private String prodCode;
    private String prodName;
    private String prodAttr;
    private Integer salePrice;
    private Integer orderQty;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
