package com.shopwizard.external.excel;

import lombok.Data;

@Data
public class Order {
    private String orderNo;
    private String orderDate;
    private String chnlName;
    private String chnlOrderNo;
    private String buyerName;
    private String buyerPhone;
    private String buyerEmail;
    private String rcpName;
    private String rcpPhone;
    private String rcpAddr;
    private String rcpZip;
    private String prodName;
    private String optionName;
    private int prodQty;
    private int prodAmt;
    private int deliAmt;
    private int totalAmt;
    private int discAmt;
    private int couponAmt;
    private int pointAmt;
    private int payAmt;
    private String payMethod;
    private String deliCompany;
    private String invoiceNo;
    private String orderStatus;
    private String memo;
}
