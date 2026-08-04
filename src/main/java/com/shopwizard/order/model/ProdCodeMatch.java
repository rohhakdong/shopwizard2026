package com.shopwizard.order.model;

import lombok.Data;

@Data
public class ProdCodeMatch {
    private String prodName;
    private String prodOption;
    private String prodCode;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
