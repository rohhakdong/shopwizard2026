package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderFeeRule {
    private String ruleNo;
    private String shopCode;
    private String chnlCode;
    private String prodCode;
    private String prodName;
    private String dealName;
    private Integer orderQtyStart;
    private Integer orderQtyEnd;
    private Integer orderAmtStart;
    private Integer orderAmtEnd;
    private Integer deliFee;
    private String deliFeeSect;
    private Integer promotFee;
    private Integer chnlFeeRate;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
