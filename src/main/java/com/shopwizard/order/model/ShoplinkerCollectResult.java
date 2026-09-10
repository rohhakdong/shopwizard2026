package com.shopwizard.order.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 샵링커 주문수집 실행 결과. 몰(=target)별 요약 목록.
 */
@Data
public class ShoplinkerCollectResult {
    private boolean ok = true;
    private String message;
    private List<MallSummary> malls = new ArrayList<>();

    @Data
    public static class MallSummary {
        private Integer transId;
        private String mallName;      // 화면 표시용 (예: "옥션 shopion1")
        private String resultType;    // 성공 / 실패
        private String resultMessag;
        private int total;
        private int success;
        private int duplicate;
        private int skip;
        private int error;
    }
}
