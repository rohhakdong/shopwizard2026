package com.shopwizard.catalog.model;

import lombok.Data;

@Data
public class ProdChnl {
    private String prodCode;
    private String chnlCode;
    private String chnlCateCode;
    private String chnlProdCode;
    private String chnlProdName;
    private Integer chnlSalePrice;
    private String chnlSupplyPrice;
    private String chnlMngrMd;
    private String chnlApprovState;
    private String chnlApprovDate;
    private String remark;
    private Integer state;
    private String registDate; private String registId; private String registName;
    private String changeDate; private String changeId; private String changeName;
}
