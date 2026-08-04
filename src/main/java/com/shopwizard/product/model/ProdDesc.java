package com.shopwizard.product.model;

import lombok.Data;

@Data
public class ProdDesc {
    private String prodCode;
    private String descCode;
    private String detailDesc;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
