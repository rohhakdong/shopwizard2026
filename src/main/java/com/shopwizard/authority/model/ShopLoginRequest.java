package com.shopwizard.authority.model;

import lombok.Data;

@Data
public class ShopLoginRequest {
    private String loginId;
    private String passwd;
}
