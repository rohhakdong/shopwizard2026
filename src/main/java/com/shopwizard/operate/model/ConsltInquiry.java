package com.shopwizard.operate.model;

import lombok.Data;

/**
 * 고객문의 전체 조회의 한 행(shopion.tOprCustConslt). 주문상세의 고객상담과 같은 테이블이고, 주문번호·상품은 문자열 컬럼이라 문자열로 읽는다.
 * 표시용 이름(코드표·채널·상품)은 LEFT JOIN 으로 채워 코드표에서 빠진 옛 값이나 채널 없는 상담도 목록에서 사라지지 않게 한다.
 */
@Data
public class ConsltInquiry {
    private Integer custConsltNo;
    private String custConsltType;
    private String custConsltTypeName;
    private String custConsltReasonType;
    private String custConsltReasonTypeName;
    private String consltMediaType;
    private String consltMediaTypeName;
    private Integer custId;
    private String custName;
    private String custEmail;
    private String custPhoneNo;
    private String custMobileNo;
    private String consltTitle;
    private String consltCntnts;
    private String consltDate;
    private String mngrId;
    private String mngrName;
    private String answrTitle;
    private String answrCntnts;
    private String answrDate;
    private String orderNo;
    private String orderProdNo;
    private String prodCode;
    private String prodName;
    private String itemName;
    private String chnlCode;
    private String chnlName;
    /** 처리 여부: 처리내용이 있으면 1 */
    private Integer answered;
}
