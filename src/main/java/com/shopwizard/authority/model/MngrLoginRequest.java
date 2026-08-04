package com.shopwizard.authority.model;

import lombok.Data;

@Data
public class MngrLoginRequest {
    private String loginId;
    private String passwd;
    private String chnlCode;
}
