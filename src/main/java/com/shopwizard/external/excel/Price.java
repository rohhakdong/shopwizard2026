package com.shopwizard.external.excel;

import lombok.Data;

@Data
public class Price {
    private String prodNo;
    private String prodName;
    private String optionName;
    private int supplyAmt;
    private int marginAmt;
    private int sellAmt;
    private int dcAmt;
    private String dcRate;
    private String useYn;
    private String remark;
}
