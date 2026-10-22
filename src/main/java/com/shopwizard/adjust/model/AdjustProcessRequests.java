package com.shopwizard.adjust.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 정산 생성·수정·마감 요청/응답 모델 모음. */
public final class AdjustProcessRequests {

    private AdjustProcessRequests() {}

    /** 정산자료 생성: 선택한 대상 행들. kind 는 목록이 내려준 값('SHIP'|'RETURN') 그대로. */
    @Data
    public static class CreateRequest {
        private List<Item> items;

        @Data
        public static class Item {
            private String kind;
            private Integer orderNo;
            private Integer orderProdNo;
            private Integer orderChangeNo;
        }
    }

    /** 정산자료 금액 수정(마감 전). */
    @Data
    public static class PriceRequest {
        private Integer adjustDetailNo;
        private Long supplyPrice;
        private Long discntSupplyPrice;
        private Long buyPrice;
        private Integer vatRate;
    }

    /** 상점 마감: 선택한 상점들을 같은 기간(yyyy-MM-dd ~ yyyy-MM-dd)으로 마감. */
    @Data
    public static class CloseRequest {
        private List<String> shopCodes;
        private String startDate;
        private String endDate;
    }

    /** 건별 결과 집계(성공 내역에는 마감 시 발급한 정산번호 등을 담는다). */
    @Data
    public static class BatchResult {
        private int success;
        private int failed;
        private List<String> successDetails = new ArrayList<>();
        private List<String> failDetails = new ArrayList<>();
    }
}
