package com.shopwizard.operate.model;

import lombok.Data;

@Data
public class EntrstProd {
    private Integer entrstNo;
    private Integer custId;
    private String chnlCode;
    private String svcCode;
    private String prodName;
    private String prodDesc;
    private String prodImg1;
    private String prodImg2;
    private String prodLabel;
    private String partDesc;
    private String buyDate;
    private String buyStore;
    private Integer buyPrice;
    private Integer saleHopePrice;
    private Integer prevSaleHopePrice;
    private String priceChangeDate;
    private String deliMethod;
    private String deliCompCode;
    private String deliExpectDate;
    private String reciptChnlCode;
    private String entrstDate;
    private String prgresState;
    private Integer salePrice;
    private String saleDate;
    private Integer saleFee;
    private Integer saleAdjustAmt;
    private String saleAdjustDate;
    private String bankAccnt;
    private String loanRqstDate;
    private Integer loanRqstAmt;
    private Integer loanFee;
    private Integer loanAdjustAmt;
    private String loanAdjustDate;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
