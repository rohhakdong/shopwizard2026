package com.shopwizard.authority.model;

import lombok.Data;

/** 창고(거래처) 계정 로그인 요청 — ShopLoginRequest/CustLoginRequest와 동일한 형태. */
@Data
public class WarehsLoginRequest {
    private String loginId;
    private String passwd;
}
