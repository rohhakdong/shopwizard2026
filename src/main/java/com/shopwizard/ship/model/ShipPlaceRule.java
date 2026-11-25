package com.shopwizard.ship.model;

import lombok.Data;

/**
 * 배송제한 규칙(shopwizard.tShpShipPlaceRule, 기본키 RuleNo 자동증가). 상점의 발주서 행 중 조건에 맞는 행의 출고지(=엑셀 시트명)를 바꾸고
 * 필요하면 수취인명 앞에 표시(★ ■ …)를 붙인다. 우편번호 구분과 주소 패턴은 <b>OR</b>, 상품명 포함·제외 채널은 <b>AND</b>,
 * 우편번호 구분과 주소 패턴이 모두 비면 상점의 모든 행이 대상이다(기본 출고지 규칙).
 */
@Data
public class ShipPlaceRule {
    private Integer ruleNo;
    private String shopCode;
    /** 조회 전용(tCmpShop 조인). */
    private String shopName;
    private String ruleName;
    /** 우편번호 구분 — tShpDeliRstrctZipcodeAddr.DeliRstrctSect. */
    private String zipSect;
    /** 주소 LIKE 패턴, 한 줄에 하나(예 {@code %경기%광주%중대동%}). */
    private String addrPatterns;
    /** 맞으면 이 규칙에서 제외할 주소 LIKE 패턴, 한 줄에 하나. */
    private String exceptAddrPatterns;
    /** 발주서 행의 상품명(발주단가표의 발주상품명칭)에 포함(비우면 조건 없음) — 채널 상품명이 아니다. */
    private String prodName;
    /** 제외 채널 코드(쉼표 구분). */
    private String exceptChnlCodes;
    /** 시트명(엑셀 시트 이름 규칙: 31자 이하, [ ] : * ? / \ 불가). */
    private String shipPlace;
    private String namePrefix;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
