package com.shopwizard.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 주문 엑셀 가격수정의 한 행 — 검사 결과(미리보기)이자 수정 결과. 한 행 = 주문 줄(주문번호+순번) 1개. */
@Data
public class OrderPriceRow {
    public static final String OK = "OK";
    public static final String SAME = "SAME";
    public static final String ERROR = "ERROR";

    /** 엑셀 행 번호(헤더가 1행). */
    private int rowNo;
    private Integer orderNo;
    private Integer orderProdNo;
    /** 현재 주문 줄의 상품명·상태(참고용, 줄을 찾았을 때만). */
    private String prodName;
    private String orderState;
    /** 바뀌는 값("판매 줄합계 2,000→2,400" 처럼). 비어 있으면 변경 없음. */
    private List<String> changes = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    /** 수정을 마쳤는가(등록 응답에서만 true). */
    private boolean applied;

    /** 수정에 쓸 엑셀 입력값 — 검사를 통과한 행만 채워진다. 응답에는 싣지 않는다. */
    @JsonIgnore
    private Input input;

    /** 오류가 있으면 ERROR, 바뀌는 값이 없으면 SAME, 아니면 OK. */
    public String getStatus() {
        if (!errors.isEmpty()) return ERROR;
        return changes.isEmpty() ? SAME : OK;
    }

    /** "주문번호-순번" — 이력의 행 식별자. */
    public String getKey() {
        return orderNo == null ? "" : orderNo + "-" + (orderProdNo == null ? 1 : orderProdNo);
    }

    public void error(String message) { errors.add(message); }
    public void warn(String message) { warnings.add(message); }

    /**
     * 엑셀에 적은 값. 칸을 비우면 null = 현재 값 유지. 판매액·공급가·원가는 수량을 곱하기 전 1개 가격이고
     * 판촉비·배송비는 줄 값 그대로다.
     */
    @Data
    public static class Input {
        private Integer saleUnit;
        private Integer supplyUnit;
        private Integer buyUnit;
        private Integer promotFeeAmt;
        private Integer deliFeeAmt;
        /** 선불 / 착불 / 무료(엑셀 원문을 레거시 규칙으로 바꾼 값). 비우면 null. */
        private String deliFeeType;
    }
}
