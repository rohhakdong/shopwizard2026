package com.shopwizard.order.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 주문 엑셀 가격수정 검사(미리보기)/수정 결과 요약. */
@Data
public class OrderPriceResult {
    /** 파일 전체 문제(읽을 수 없음, 필수 헤더 없음, 행 수 초과 등). 있으면 rows 는 비어 있다. */
    private String fileError;
    private List<OrderPriceRow> rows = new ArrayList<>();
    /** 바뀌는 값이 있는 정상 행 수. */
    private int okCount;
    /** 바뀌는 값이 없는 행 수. */
    private int sameCount;
    private int errorCount;
    /** 경고가 있는 행 수. */
    private int warningCount;
    /** 이번 요청에서 실제로 수정한 행 수(검사는 항상 0). */
    private int appliedCount;
    /** 이번 요청에서 DB 반영을 시도했는가(검사는 항상 false). */
    private boolean imported;
    private Integer uploadId;
    private String message;

    /** 바뀌는 값이 있는 정상 행이 1건 이상이면 등록할 수 있다(오류·변경 없음 행은 건너뛴다). */
    public boolean isImportable() { return fileError == null && okCount > 0; }

    public static OrderPriceResult ofFileError(String message) {
        OrderPriceResult r = new OrderPriceResult();
        r.fileError = message;
        return r;
    }

    public void summarize() {
        okCount = 0; sameCount = 0; errorCount = 0; warningCount = 0; appliedCount = 0;
        for (OrderPriceRow row : rows) {
            switch (row.getStatus()) {
                case OrderPriceRow.ERROR -> errorCount++;
                case OrderPriceRow.SAME -> sameCount++;
                default -> okCount++;
            }
            if (!row.getWarnings().isEmpty()) warningCount++;
            if (row.isApplied()) appliedCount++;
        }
    }
}
