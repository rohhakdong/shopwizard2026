package com.shopwizard.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 엑셀 일괄등록의 한 행 — 검사 결과(미리보기)이자 등록 결과. */
@Data
public class ProdExcelRow {
    public static final String NEW = "NEW";
    public static final String UPDATE = "UPDATE";

    /** 엑셀 행 번호(헤더가 1행). 사용자가 파일에서 바로 찾을 수 있게 엑셀 기준 번호를 쓴다. */
    private int rowNo;
    /** NEW(신규 등록) / UPDATE(기존 상품 수정). */
    private String action;
    /** 수정이면 파일의 상품코드, 신규는 등록이 끝난 뒤 부여된 코드(미리보기에서는 비어 있다). */
    private String prodCode;
    private String prodName;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    /** 등록에 쓸 값 — 검사를 통과한 행만 채워진다. 응답에는 싣지 않는다. */
    @JsonIgnore
    private Prod prod;

    public String getStatus() { return errors.isEmpty() ? "OK" : "ERROR"; }

    public void error(String message) { errors.add(message); }
    public void warn(String message) { warnings.add(message); }
}
