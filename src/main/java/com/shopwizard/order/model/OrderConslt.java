package com.shopwizard.order.model;

import lombok.Data;

/**
 * 주문에 딸린 고객상담 한 건(shopion.tOprCustConslt). 레거시 주문상세의 "상담내역".
 * OrderNo·OrderProdNo 는 테이블이 문자열 컬럼이라 문자열로 읽고 쓴다(OrderProdNo 가 비면 주문 전체에 대한 상담).
 */
@Data
public class OrderConslt {
    private Integer custConsltNo;
    private String custConsltType;
    private String custConsltReasonType;
    private String consltMediaType;
    private Integer custId;
    private String custName;
    private String custEmail;
    private String custPhoneNo;
    private String custMobileNo;
    private String consltChnlCode;
    private String consltTitle;
    private String consltCntnts;
    private String consltDate;
    private String mngrId;
    private String mngrName;
    private String mngrEmail;
    private String answrTitle;
    private String answrCntnts;
    private String answrDate;
    private String orderNo;
    private String orderProdNo;
    private String orderChangeNo;
    private String prodCode;
    private String itemCode;
    private String itemName;
    private String chnlCode;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;

    // 조회에서만 채우는 표시용 값(코드표 이름·상품명)
    private String custConsltTypeName;
    private String custConsltReasonTypeName;
    private String consltMediaTypeName;
    private String prodName;
}
