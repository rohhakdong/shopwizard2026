package com.shopwizard.order.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 주문 엑셀 검사(미리보기)/등록 결과 요약. */
@Data
public class OrderExcelResult {
    /** 파일 전체 문제(읽을 수 없음, 필수 헤더 없음, 행 수 초과 등). 있으면 rows 는 비어 있다. */
    private String fileError;
    private List<OrderExcelRow> rows = new ArrayList<>();
    /** 등록할 수 있는 행 수(오류도 중복도 아닌 행). 등록 뒤에는 등록하지 못한 행이 빠진 값이다. */
    private int okCount;
    private int dupCount;
    private int errorCount;
    /** 경고가 있는 행 수. */
    private int warningCount;
    /** 이번 요청에서 실제로 주문을 만든 행 수(검사는 항상 0). */
    private int createdCount;
    /** 이번 요청에서 DB 에 반영을 시도했는가(검사는 항상 false). */
    private boolean imported;
    /** 업로드 이력 아이디(등록할 때만). */
    private Integer uploadId;
    private String message;

    /** 정상 행이 1건 이상이면 등록할 수 있다(오류·중복 행은 건너뛴다). */
    public boolean isImportable() { return fileError == null && okCount > 0; }

    public static OrderExcelResult ofFileError(String message) {
        OrderExcelResult r = new OrderExcelResult();
        r.fileError = message;
        return r;
    }

    public void summarize() {
        okCount = 0; dupCount = 0; errorCount = 0; warningCount = 0; createdCount = 0;
        for (OrderExcelRow row : rows) {
            switch (row.getStatus()) {
                case OrderExcelRow.ERROR -> errorCount++;
                case OrderExcelRow.DUP -> dupCount++;
                default -> okCount++;
            }
            if (!row.getWarnings().isEmpty()) warningCount++;
            if (row.getOrderNo() != null) createdCount++;
        }
    }
}
