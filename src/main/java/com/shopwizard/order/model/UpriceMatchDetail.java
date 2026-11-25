package com.shopwizard.order.model;

import lombok.Data;

/**
 * 발주단가 매칭 상세 1줄 (테이블 shopwizard.tOrdUpriceMatch, PK=ProdName+ItemName+MatchSeq).
 * 채널 상품명(ProdName+ItemName) 하나가 세트/묶음이면 여러 줄로, 단품이면 1줄로 저장된다.
 * keywrd/capa/uprice 는 tOrdOrderUprice 조인 결과(조회 전용) — insert 시에는 쓰지 않는다.
 */
@Data
public class UpriceMatchDetail {
    private String prodName;
    private String itemName;
    private Integer matchSeq;
    private String matchUpriceCode;
    private Integer qty;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;

    // 조회 전용 (tOrdOrderUprice 조인 결과)
    private String keywrd;
    private String capa;
    private Integer uprice;
    private String unitName;
    /** 발주단가표의 사용여부(코드가 없으면 null) — 규칙 편집 화면에서 사용 중지·삭제된 코드를 알리는 데 쓴다. */
    private Integer upriceUseYn;
}
