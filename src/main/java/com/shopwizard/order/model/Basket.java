package com.shopwizard.order.model;

import lombok.Data;

@Data
public class Basket {
    private Integer basketNo;
    private Integer custId;
    private String sessonId;
    private String prodCode;
    private String shopProdCode;
    private String prodName;
    private String prodImg;
    private String itemCode;
    private String itemName;
    private Integer prodQty;
    private Integer listPrice;
    private Integer salePrice;
    private Integer supplyPrice;
    private Integer buyPrice;
    private Integer vatRate;
    private Integer vatAmt;
    private Integer deliFeeId;
    private String deliFeeType;
    private Integer deliFeeAmt;
    private String directOrderYn;
    private String chnlCode;
    private String shopCode;
    private String mroYn;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
    // join fields
    private String itemDesc;
    private Integer custCpnId;
    private Integer custCpnDiscntAmt;
    private Integer prodCpnId;
    private Integer prodCpnDiscntAmt;
    private Integer pointSaveAmt;
}
