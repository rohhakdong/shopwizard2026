package com.shopwizard.order.model;

import lombok.Data;

/**
 * 샵링커 주문수집 실행 이력 1건 (테이블 {@code shopion.tOrdSlnkTrans}).
 * 몰 하나 + 기간 하나가 1행. 레거시 스키마와 동일하게 매핑한다.
 *
 * - {@code mallCode} : 샵링커 오픈마켓 구분 — "" 오픈마켓제외 쇼핑몰 / "A" 옥션 / "G" 지마켓(법인) /
 *   "R" 지마켓(개인) / "11" 11번가
 * - {@code loginId}  : 오픈마켓 아이디 (예: shopion1, plus825 …). 오픈마켓제외 쇼핑몰이면 "".
 * - {@code resultType} : 준비 / 진행 / 성공 / 실패
 */
@Data
public class SlnkTrans {
    private Integer transId;
    private String startDate;       // 수집 대상 시작일 (yyyy-MM-dd)
    private String endDate;         // 수집 대상 종료일 (yyyy-MM-dd)
    private String mallCode;
    private String loginId;
    private String resultType;
    private String resultMessag;
    private String fileLoc;
    private String transStartDate;  // 실행 시작 일시
    private String transEndDate;    // 실행 종료 일시
    private String shoplinkerUrl;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;

    // 화면 표시용(조인 아님) — 몰 이름
    private String mallName;
}
