package com.shopwizard.order.model;

import lombok.Data;

import java.util.List;

/**
 * 주문취소접수 처리 요청 — 선택한 주문라인들을 주문취소로 전환한다. 사유는 배치 전체에 공통 적용.
 */
@Data
public class OrderCancelRequest {
    private List<Line> lines;
    private String reason;
    private String reasonDetail;
    private String registId;
    private String registName;

    @Data
    public static class Line {
        private Integer orderNo;
        private Integer orderProdNo;
    }
}
