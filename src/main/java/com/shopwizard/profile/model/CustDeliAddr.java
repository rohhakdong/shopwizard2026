package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class CustDeliAddr {
    private Integer custId;
    private String custName;
    private Integer deliAddrNo;
    private Integer defaltYn;
    private String recverName;
    private String zipcode;
    private String addr1;
    private String addr2;
    private String homePhoneNo;
    private String mobilePhoneNo;
    private String mobileSvc;
    private String chnlCode;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
