package com.shopwizard.ship.model;

import lombok.Data;

import java.util.List;

/**
 * 반품요청 접수 처리 요청 — 선택한 배송완료 주문라인들을 반품요청으로 전환한다.
 * 사유는 배치 전체에 공통 적용.
 */
@Data
public class ReturnRequestBatchRequest {
    private List<Line> lines;
    private String returnReason;
    private String returnReasonDetail;
    private String registId;
    private String registName;

    @Data
    public static class Line {
        private Integer orderNo;
        private Integer orderProdNo;
    }
}
