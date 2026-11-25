package com.shopwizard.order.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 배송비·판촉비 규칙(shopwizard.tOrdOrderFeeRule, 기본키 RuleNo 자동증가). 상점 단위 규칙이며 조건(채널·상품명·딜명·수량·금액 구간)에
 * 맞는 지불완료 주문 라인에 배송비/판촉비를 정하거나(수수료율 0) 채널 수수료율로 공급가를 다시 계산한다(수수료율 ≠ 0).
 * 구간의 0(또는 비움)은 "제한 없음". 테이블에는 상품코드 컬럼이 없다(레거시 sqlmap 에는 있었지만 실제 DB 에는 없다).
 */
@Data
public class OrderFeeRule {
    private Integer ruleNo;
    private String shopCode;
    /** 조회 전용(tCmpShop 조인). */
    private String shopName;
    private String chnlCode;
    /** 조회 전용(tCmpChnl 조인). */
    private String chnlName;
    private String prodName;
    private String dealName;
    private Integer orderQtyStart;
    private Integer orderQtyEnd;
    private Integer orderAmtStart;
    private Integer orderAmtEnd;
    private Integer deliFee;
    /** 전체무료 / 무료 / 선불 / 착불 — 규칙이 적용될 배송형태(전체무료는 형태와 상관없이 적용하고 무료로 바꾼다). */
    private String deliFeeSect;
    private Integer promotFee;
    /** 채널 수수료율(%) — 0 이 아니면 공급가 = 판매가 x (100 - 수수료율) / 100 만 적용한다. */
    private BigDecimal chnlFeeRate;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
