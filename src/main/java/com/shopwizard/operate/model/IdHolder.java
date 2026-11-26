package com.shopwizard.operate.model;

import lombok.Data;

/** INSERT 가 만든 자동증가 번호를 돌려받는 그릇(MyBatis keyProperty). */
@Data
public class IdHolder {
    private Integer id;
}
