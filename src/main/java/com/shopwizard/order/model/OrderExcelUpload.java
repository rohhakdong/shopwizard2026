package com.shopwizard.order.model;

import lombok.Data;

/** 주문 엑셀 업로드 이력 한 건(shopion.tOrdExcelUpload). 레거시 "주문엑셀등록" 화면의 마스터 행. */
@Data
public class OrderExcelUpload {
    private Integer uploadId;
    /** 진행 / 성공. */
    private String resultType;
    private String resultMessag;
    /** 올린 파일 이름. */
    private String fileLoc;
    private String uploadStartDate;
    private String uploadEndDate;
    private String registId;
    private String registName;
}
