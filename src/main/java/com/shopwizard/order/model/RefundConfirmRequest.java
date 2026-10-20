package com.shopwizard.order.model;

import lombok.Data;

import java.util.List;

/**
 * 환불확정 처리 요청 — 선택한 주문라인들(주문취소/반품완료)을 환불완료로 전환한다.
 * {@code skipPg} 가 true 이면 토스 결제 취소 API 를 호출하지 않고 상태만 바꾼다(가상계좌처럼 자동 취소가
 * 불가능하거나 이미 토스 대시보드에서 별도로 환불한 건).
 */
@Data
public class RefundConfirmRequest {
    private List<Line> lines;
    private boolean skipPg;
    private String registId;
    private String registName;

    @Data
    public static class Line {
        private Integer orderNo;
        private Integer orderProdNo;
    }
}
