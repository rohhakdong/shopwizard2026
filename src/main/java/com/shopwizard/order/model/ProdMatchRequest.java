package com.shopwizard.order.model;

import lombok.Data;

/**
 * 상품 매칭 요청 — (상품명, 옵션) 이 같은 미매칭 주문라인을 실제 상품에 연결한다.
 */
@Data
public class ProdMatchRequest {
    private String prodName;        // 미매칭 주문라인의 ProdName
    private String prodOption;      // 미매칭 주문라인의 ItemName(옵션). 없으면 ""
    private String prodCode;        // 연결할 실제 상품코드 (화면에서 고른 값)
    private String shopProdCode;    // 대안: 상점상품코드로 조회 (CSV 업로드용)
    private boolean saveRule = true;// tOrdProdCodeMatch 에 재사용 규칙 저장
    private String svcCode;         // 관리자 서비스코드 (프론트에서 info.svcCode)
    // backfill 대상 범위 — 화면에서 보고 있던 검색조건 그대로. 이 범위 안의 라인만 반영한다
    // (범위를 안 주면 해당 상품명의 미매칭 라인 전체가 대상이 되어 과거 주문까지 건드리므로 필수).
    private String pStartDate;
    private String pEndDate;
    private String pChnlCode;
    private String registId;
    private String registName;
}
