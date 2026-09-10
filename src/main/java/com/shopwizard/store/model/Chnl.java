package com.shopwizard.store.model;

import lombok.Data;

@Data
public class Chnl {
    private String chnlCode;
    private String chnlName;
    private String chnlDesc;
    private String chnlUrl;
    private String chnlPolicyCode;
    private String svcCode;
    private Integer designId;
    private String compCode;
    private String logoImg;
    private String mngrName;
    private String mngrPhoneNo;
    private String mngrMobileNo;
    private String mngrFaxNo;
    private String mngrEmail;
    private String mallStartDate;
    private String mallEndDate;
    private String memberChnlCode;
    private String prsdntChnlCode;
    private String adjustChnlCode;
    private String slnkMallCode;   // 샵링커 제휴몰명 (샵링커 주문수집 → 채널 매핑 기준)
    private String loginId;        // 샵링커 제휴몰 아이디
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
