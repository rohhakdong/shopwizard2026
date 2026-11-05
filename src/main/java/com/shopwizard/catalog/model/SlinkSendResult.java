package com.shopwizard.catalog.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 샵링커 전송 결과 — 상품별 한 행. */
@Data
public class SlinkSendResult {
    public static final String SENT = "SENT";        // 실제로 보냈고 성공
    public static final String FAILED = "FAILED";    // 보냈지만 실패(샵링커 거절·오류·타임아웃)
    public static final String DRY = "DRY";          // 시험 모드 — 실제로 보내지 않음
    public static final String SKIPPED = "SKIPPED";  // 조건 미달로 호출하지 않음

    /** dry-run | live — 이 요청이 어느 모드로 처리됐는지. */
    private String mode;
    private List<Row> results = new ArrayList<>();

    @Data
    public static class Row {
        private String prodCode;
        private String prodName;
        private String status;
        private String message;

        public Row(String prodCode) { this.prodCode = prodCode; }
    }

    public long count(String status) { return results.stream().filter(r -> status.equals(r.getStatus())).count(); }
}
