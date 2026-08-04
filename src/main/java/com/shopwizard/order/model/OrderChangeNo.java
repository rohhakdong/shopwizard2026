package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderChangeNo {
    private Integer orderNo;
    private Integer orderProdNo;
    private Integer orderChangeNo;
    private String prevState;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
}
