package com.shopwizard.ship.model;

import lombok.Data;

import java.util.List;

/**
 * 발주확인 처리 요청 — 선택한 출고지시 상태 건들을 발주확인으로 전환한다.
 */
@Data
public class ShipDirectCheckRequest {
    private List<Line> lines;
    private String registId;
    private String registName;

    @Data
    public static class Line {
        private Integer orderNo;
        private Integer orderProdNo;
        private Integer orderChangeNo;
    }
}
