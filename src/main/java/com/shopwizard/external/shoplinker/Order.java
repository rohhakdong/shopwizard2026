package com.shopwizard.external.shoplinker;

import lombok.Data;

@Data
public class Order {
    private String code;
    private String orderdate;
    private String mallcode;
    private String stcode;
    private String orderno;
    private String goodNo;
    private String goodname;
    private String optionname;
    private String optioncode;
    private int goodqty;
    private int goodprice;
    private int totalprice;
    private String buyerName;
    private String buyerHp;
    private String buyerEmail;
    private String rcpName;
    private String rcpHp;
    private String rcpZip;
    private String rcpAddr1;
    private String rcpAddr2;
    private String transNo;
    private String transCorp;
    private String paymentDt;
    private String deliveryDt;
    private String orderStatus;
    private String remark;
}
