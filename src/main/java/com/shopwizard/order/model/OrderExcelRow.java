package com.shopwizard.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shopwizard.product.model.Prod;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 주문 엑셀 업로드의 한 행 — 검사 결과(미리보기)이자 등록 결과. 한 행 = 주문 1건(상품 1줄). */
@Data
public class OrderExcelRow {
    public static final String OK = "OK";
    public static final String DUP = "DUP";
    public static final String ERROR = "ERROR";

    /** 엑셀 행 번호(헤더가 1행). */
    private int rowNo;
    /** 엑셀의 "주문번호" — 중복 판정 기준(레거시 tOrdOrderExcel PK). */
    private String excelOrderNo;
    private String chnlName;
    private String prodName;
    /** 이미 처리된 엑셀 주문번호인가(또는 등록 도중 같은 번호가 먼저 들어간 경우). */
    private boolean duplicate;
    /** 등록이 끝난 뒤 새로 만들어진 주문번호(검사에서는 비어 있다). */
    private Integer orderNo;
    private String resultMessage;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    /** 등록에 쓸 값 — 검사를 통과한 행만 채워진다. 응답에는 싣지 않는다. */
    @JsonIgnore
    private Parsed parsed;

    /** 오류가 있으면 ERROR, 아니면 중복이면 DUP, 아니면 OK. */
    public String getStatus() {
        if (!errors.isEmpty()) return ERROR;
        return duplicate ? DUP : OK;
    }

    public void error(String message) { errors.add(message); }
    public void warn(String message) { warnings.add(message); }

    /** 검사를 통과한 한 행의 값. 금액은 엑셀의 1개 단가 그대로이고, 저장할 때 수량을 곱한다. */
    @Data
    public static class Parsed {
        private String excelOrderNo;
        private String chnlName;
        private String chnlCode;
        private String chnlOrderNo;
        private String orderName;
        private String orderPhoneNo;
        private String orderMobileNo;
        private String recverName;
        private String recverPhoneNo;
        private String recverMobileNo;
        private String recverZipcode;
        private String recverAddr;
        private String deliMemo;
        private String shopProdCode;
        private String prodCode;
        private String prodName;
        private String shopName;
        private String optionName;
        private int supplyPrice;
        private int salePrice;
        private int buyPrice;
        private int qty;
        private LocalDate orderDate;
        /** 엑셀에 적힌 배송비 타입 원문(원본 보관용). */
        private String deliFeeTypeRaw;
        /** 선불 / 착불 / 무료. */
        private String deliFeeType;
        private int deliFeeAmt;
        private int promotFeeAmt;
        /** 매칭된 판매 상품 — 없으면 null(미매칭 저장). */
        private Prod matched;
    }
}
