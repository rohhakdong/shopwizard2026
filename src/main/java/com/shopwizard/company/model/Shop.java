package com.shopwizard.company.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Shop {
    private String shopCode;
    private String supplyName;
    private String supplyCode;
    private String svcCode;
    private String compCode;
    private String shopName;
    private int autoOrderYn;
    private String applyStartDate;
    private String applyEndDate;
    private String adjustPeriodCode;
    private String loginId;
    // 해시를 응답으로 내보내지 않는다(요청 바디로 받기만 한다)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwd;
    private String mngrName;
    private String phoneNo;
    private String mobileNo;
    private String faxNo;
    private String email;
    private String brandIntro;
    private String patentCode;
    private String lndryMethod;
    private String asDesc;
    private String prodDesc;
    private String mngrMd;
    private int brandCode;
    private int makerCode;
    private int makeNationCode;
    private int originCode;
    private int vatRate;
    private String sexualType;
    private int deliCompId;
    private int leadTime;
    private String visualImg;
    private String sizeChart;
    private String approvDate;
    private String passwdChangeDate;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
