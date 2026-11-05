package com.shopwizard.catalog.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 엑셀 검사(미리보기)/등록 결과 요약. */
@Data
public class ProdExcelResult {
    /** 파일 전체 문제(읽을 수 없음, 필수 헤더 없음, 행 수 초과 등). 있으면 rows 는 비어 있다. */
    private String fileError;
    private List<ProdExcelRow> rows = new ArrayList<>();
    private int newCount;
    private int updateCount;
    /** 오류가 있는 행 수. */
    private int errorCount;
    /** 경고가 있는 행 수. */
    private int warningCount;
    /** 이번 요청에서 실제로 DB 에 반영했는가(미리보기는 항상 false). */
    private boolean imported;

    /** 오류가 하나도 없고 행이 1건 이상이면 등록할 수 있다. */
    public boolean isImportable() { return fileError == null && errorCount == 0 && !rows.isEmpty(); }

    public static ProdExcelResult ofFileError(String message) {
        ProdExcelResult r = new ProdExcelResult();
        r.fileError = message;
        return r;
    }

    public void summarize() {
        newCount = 0; updateCount = 0; errorCount = 0; warningCount = 0;
        for (ProdExcelRow row : rows) {
            if (ProdExcelRow.NEW.equals(row.getAction())) newCount++;
            else if (ProdExcelRow.UPDATE.equals(row.getAction())) updateCount++;
            if (!row.getErrors().isEmpty()) errorCount++;
            if (!row.getWarnings().isEmpty()) warningCount++;
        }
    }
}
