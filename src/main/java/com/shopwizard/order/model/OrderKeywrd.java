package com.shopwizard.order.model;

import lombok.Data;

/**
 * 발주 상품명 키워드(shopwizard.tOrdOrderKeywrd, 기본키 KeywrdSeq 자동증가). 상점 미지정 주문 라인을 자동발주에서 그 상점으로 가져오는 기준이다.
 * KeywrdSect: LIKE(상품명에 포함) / LIKE OPTION(옵션명에 포함) / NOT LIKE · NOT LIKE OPTION(포함되면 제외).
 */
@Data
public class OrderKeywrd {
    private Integer keywrdSeq;
    private String keywrdName;
    private String keywrdSect;
    private String shopCode;
    /** 조회 전용(tCmpShop 조인). */
    private String shopName;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
