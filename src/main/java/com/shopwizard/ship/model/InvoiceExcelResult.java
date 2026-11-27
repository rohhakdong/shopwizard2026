package com.shopwizard.ship.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 송장 엑셀 일괄 입력의 검사(preview)·등록(import) 결과. */
@Data
public class InvoiceExcelResult {
    /** 파일 자체를 읽을 수 없을 때의 사유(있으면 rows 는 비어 있다). */
    private String fileError;
    private List<Row> rows = new ArrayList<>();
    private int okCount;
    private int errorCount;
    /** 등록(import)에서 실제로 저장한 줄 수. */
    private int savedCount;
    /** 등록을 마쳤는가(검사만 했으면 false). */
    private boolean imported;
    /** 등록할 수 있는 정상 행이 하나라도 있는가. */
    private boolean importable;
    private String message;
    /** 샵링커 전송 모드: dry-run(시험, 실제로 보내지 않음) 또는 live. */
    private String slnkMode;

    @Data
    public static class Row {
        private int rowNo;
        private Integer orderNo;
        private Integer orderProdNo;
        private Integer orderChangeNo;
        private String deliCompName;
        private String invNo;
        /** OK · ERROR */
        private String status;
        private List<String> errors = new ArrayList<>();
        /** 등록 결과: 저장했으면 "저장 완료". */
        private String resultMessage;
        /** 샵링커 전송 결과(sent·failed·dry·skip)와 설명 — 등록 때만 채운다. */
        private String slnkStatus;
        private String slnkMessage;
    }
}
