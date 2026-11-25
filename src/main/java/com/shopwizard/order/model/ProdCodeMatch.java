package com.shopwizard.order.model;

import lombok.Data;

@Data
public class ProdCodeMatch {
    private String prodName;
    private String prodOption;
    private String prodCode;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;

    // 조회 전용: 규칙이 가리키는 판매 상품(없으면 null)
    private String matchedProdName;
    private String shopCode;
    private String shopName;
}
