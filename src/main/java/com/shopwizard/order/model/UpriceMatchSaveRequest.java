package com.shopwizard.order.model;

import lombok.Data;

import java.util.List;

/** 발주단가매칭 저장 요청 (POST /order/uprice-match/save). */
@Data
public class UpriceMatchSaveRequest {
    private String prodName;
    private String itemName;
    private List<UpriceMatchDetail> lines;
    private String registId;
    private String registName;
}
