package com.shopwizard.authority.model;

import lombok.Data;

@Data
public class CustLoginRequest {
    private String loginId;
    private String passwd;
    private String chnlCode;
    private boolean useMd5;  // scarabe, dcgolfmall 채널
}
