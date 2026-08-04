package com.shopwizard.order.model;

import lombok.Data;

@Data
public class ProdOptionChange {
    private String prodCode;
    private String prodName;
    private String prodOption;
    private String prodQty;
    private String prodOptionChange;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
