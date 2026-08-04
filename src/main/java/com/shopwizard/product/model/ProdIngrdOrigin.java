package com.shopwizard.product.model;

import lombok.Data;

@Data
public class ProdIngrdOrigin {
    private String prodCode;
    private String ingrdName;
    private String ingrdOriginName;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
