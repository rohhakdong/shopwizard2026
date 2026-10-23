package com.shopwizard.company.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Warehs {
    private String warehsCode;
    private String warehsName;
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
    private String passwdChangeDate;
    private String svcCode;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
