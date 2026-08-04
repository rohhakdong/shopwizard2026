package com.shopwizard.order.model;

import lombok.Data;

@Data
public class CurrOrderProd {
    private String orderNo;
    private String orderProdNo;
    private Integer prodQty;
    private Integer salePrice;
    private Integer supplyPrice;
    private String chnlName;
    private String prevProdName;
    private String prevItemName;
    private String eventName;
    private String prodName;
    private Integer priceGiftQty;
    private Integer itemGiftQty;
    private Integer bundleYn;
    private String keywrd;
    private String capa;
    private Integer totalQty;
    private String matchProdCode;
    private Integer buyPrice;
    private Integer totalBuyPrice;
    private String matchProdSect;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
