package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderGiftRule {
    private String ruleNo;
    private String shopCode;
    private String chnlCode;
    private String chnlProdCode;
    private String chnlProdName;
    private String dealName;
    private Integer totalOrderQtyStart;
    private Integer totalOrderQtyEnd;
    private Integer totalOrderAmtStart;
    private Integer totalOrderAmtEnd;
    private String giftProdCode1;
    private String giftProdName1;
    private String giftProdCode2;
    private String giftProdName2;
    private String giftProdCode3;
    private String giftProdName3;
    private String giftProdCode4;
    private String giftProdName4;
    private String giftProdCode5;
    private String giftProdName5;
    private Integer giftQty;
    private Integer promotFee;
    private Integer dupYn;
    private String giftStartDate;
    private String giftEndDate;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
