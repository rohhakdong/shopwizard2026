package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderDetlStats {
    private String chnlCode;
    private String chnlName;
    private String shopCode;
    private String shopName;
    private String prodCode;
    private String shopProdCode;
    private String prodName;
    private Integer cnt;
    private Integer prodQty;
    private Integer salePrice;
    private Integer supplyPrice;
    private Integer buyPrice;
    private Integer prodMargin;
    private Integer chnlMargin;
    private Integer netMargin;
    private Double prodMarginRate;
    private Double chnlMarginRate;
    private Double netMarginRate;
    private Integer deliFeeAmt;
    private Integer promotFeeAmt;
    private String startDate;
    private String endDate;
    private String chnlMDName;
    private String shopMDName;
}
