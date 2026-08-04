package com.shopwizard.company.model;

import lombok.Data;

@Data
public class ChnlPolicy {
    private String chnlPolicyCode;
    private String chnlPolicyName;
    private String chnlPolicyDesc;
    private String designCode;
    private String memberChnlCode;
    private String prsdntChnlCode;
    private String adjustChnlCode;
    private String saleFeeRate;
    private String marginSharRate;
    private int marginSharStndrdAmt;
    private String trnsfrFeeRate;
    private String cardFeeRate;
    private String hppmFeeRate;
    private int prodLinkYn;
    private int orderLinkYn;
    private String linkTermnlCode;
    private String cpnApplyYn;
    private String pointUseYn;
    private String pointLimitAmt;
    private String pointSaveYn;
    private String freeApplyYn;
    private String freeApplyMonth;
    private String freeApplyAmt;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
