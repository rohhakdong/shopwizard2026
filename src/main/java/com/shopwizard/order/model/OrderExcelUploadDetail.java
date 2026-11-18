package com.shopwizard.order.model;

import lombok.Data;

/** 주문 엑셀 업로드 이력의 행별 결과(shopion.tOrdExcelUploadDetail). */
@Data
public class OrderExcelUploadDetail {
    private Integer uploadId;
    /** 엑셀 행 번호. */
    private Integer uploadDetailNo;
    private String excelOrderNo;
    /** 성공 / 중복 / 오류. */
    private String resultType;
    private String resultMessag;
    private String registId;
    private String registName;
}
