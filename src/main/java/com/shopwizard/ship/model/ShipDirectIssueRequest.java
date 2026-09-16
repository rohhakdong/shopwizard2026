package com.shopwizard.ship.model;

import lombok.Data;

import java.util.List;

/**
 * 출고지시 처리 요청 — 선택한 지불완료 주문라인들을 출고지시로 전환한다.
 */
@Data
public class ShipDirectIssueRequest {
    private List<Line> lines;
    private String registId;
    private String registName;

    @Data
    public static class Line {
        private Integer orderNo;
        private Integer orderProdNo;
    }
}
