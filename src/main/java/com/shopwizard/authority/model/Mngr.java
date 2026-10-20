package com.shopwizard.authority.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Mngr {
    private String mngrUid;
    private String mngrName;
    private String roleUid;
    private String roleName;
    private String svcCode;
    private String svcName;
    private String compCode;
    private String deptCode;
    private String empNo;
    private String loginId;
    /** 입력(요청 바디)으로만 받고 응답 JSON에는 절대 싣지 않는다. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwd;
    private String email;
    private String mobileNo;
    private String phoneNo;
    private String chnlCode;
    private String shopCode;
    private String warehsCode;
    private int prodApprovYn;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
