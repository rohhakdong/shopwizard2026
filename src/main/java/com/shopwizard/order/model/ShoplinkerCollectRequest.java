package com.shopwizard.order.model;

import lombok.Data;

import java.util.List;

/**
 * 샵링커 주문수집 실행 요청 (POST /order/shoplinker/collect).
 */
@Data
public class ShoplinkerCollectRequest {
    private String startDate;      // yyyy-MM-dd
    private String endDate;        // yyyy-MM-dd
    private List<MallTarget> malls;
    private String registId;       // 실행 관리자 (프론트에서 info.loginId)
    private String registName;     // 실행 관리자 이름 (info.name)

    @Data
    public static class MallTarget {
        private String mallCode;   // "" | "A" | "G" | "R" | "11"
        private String loginId;    // "" | "shopion1" | "plus825" | ...
    }
}
