package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderPay {
    private Integer orderNo;
    private Integer payNo;
    private String payType;
    private String payTypeName;
    private String payDate;
    private Integer payAmt;
    private Integer payFee;
    private String cardComp;
    private String approvNo;
    private String approvDate;
    private String reciptBank;
    private String reciptName;
    private String reciptNo;
    private String reciptDate;
    /** 가상계좌 번호(ReciptAccnt) — 읽을 때 쓴다. 저장은 기존 관례대로 reciptNo 로 넘긴다. */
    private String reciptAccnt;
    /** 입금 금액(ReciptAmt) */
    private java.math.BigDecimal reciptAmt;
    /** 가상계좌 입금 예정 금액 / 입금 마감 / 입금자명 */
    private java.math.BigDecimal payExpectAmt;
    private String payExpectDate;
    private String payExpectName;
    private String pgCode;
    private String pgOrderNo;
    private String pgTradeNo;
    private String payCompName;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
