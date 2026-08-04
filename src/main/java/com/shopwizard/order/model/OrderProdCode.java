package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderProdCode {
    private String matchProdCode;
    private String shopCode;
    private String prodCode;
    private String prodName;
    private String keywrd;
    private String capa1;
    private String qty1;
    private String capa2;
    private String qty2;
    private Integer useYn;
    private String resultType;
    private String resultMessag;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
