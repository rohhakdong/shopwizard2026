package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderWork {
    private String orderNo;
    private String orderName;
    private String recverName;
    private String chnlOrderNo;
    private String shopCode;
    private String shopProdCode;
    private String prodName;
    private String itemName;
    private Integer prodQty;
    private Integer vatRate;
    private Integer salePrice;
    private Integer supplyPrice;
    private Integer buyPrice;
    private String deliFeeType;
    private Integer deliFeeAmt;
    private Integer promotFeeAmt;
    private String chnlName;
    private String deliMemo;
    private String giftDesc;
    private String shipPlace;
    private Integer bundleYn;
    private String eventName;
    private String codeCheckYn;
}
