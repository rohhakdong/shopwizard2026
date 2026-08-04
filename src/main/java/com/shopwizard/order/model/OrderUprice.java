package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderUprice {
    private String matchUpriceCode;
    private String shopCode;
    private String orderProdName;
    private String keywrd;
    private String capa;
    private String uprice;
    private String unitName;
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
