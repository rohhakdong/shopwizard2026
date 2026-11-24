package com.shopwizard.order.model;

import lombok.Data;

/**
 * 주문상세의 고객상담 등록/처리내용 수정 요청. 등록은 orderNo·상담구분·사유·내용이 필수이고, 처리 수정은 custConsltNo·answrCntnts 만 쓴다.
 * 감사값(registId 등)은 받지 않는다 — 담당자는 세션 행위자다.
 */
@Data
public class OrderConsltRequest {
    private Integer custConsltNo;
    private Integer orderNo;
    /** 상담 대상 줄 — 비우면 주문 전체. */
    private Integer orderProdNo;
    private String custConsltType;
    private String custConsltReasonType;
    private String consltMediaType;
    private String consltCntnts;
    private String answrCntnts;
    /** 비우면 주문자 이름/연락처. */
    private String custName;
    private String custMobileNo;
    private String custPhoneNo;
}
