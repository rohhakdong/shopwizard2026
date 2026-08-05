package com.shopwizard.order.model;

import lombok.Data;

/**
 * tOrdOrderPayLogToss - 토스페이먼츠 결제 승인 응답 로그.
 * 기존 tOrdOrderPayLog(KCP 전용 콜백 로그)와는 컬럼 체계가 달라 별도 테이블로 분리.
 */
@Data
public class OrderPayLogToss {
    private Integer logId;
    private Integer orderNo;
    private String paymentKey;
    private String orderId;
    private String method;
    private String status;
    private Long totalAmount;
    private Long balanceAmount;
    private Long suppliedAmount;
    private Long vat;
    private String currency;
    private String requestedAt;
    private String approvedAt;
    private String cardCompany;
    private String cardNumber;
    private String cardApproveNo;
    private Integer cardInstallmentPlanMonths;
    private String receiptUrl;
    private Boolean success;
    private String resultCode;
    private String resultMsg;
    private String rawResponse;
    private String remark;
    private String registId;
    private String registName;
}
