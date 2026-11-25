package com.shopwizard.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 레거시 발주단가매칭 → 새 tOrdUpriceMatch 이관용 조회(전부 읽기 전용). */
@Mapper
public interface LegacyUpriceMigrationMapper {

    /**
     * 레거시가 주문 라인마다 풀어 둔 구성(tOrdCurrOrderProd)을 원본 주문 라인과 조인한 행.
     * 새 키는 원본 라인의 상품명·옵션(OP.ProdName/ItemName)이고, 수량은 레거시 TotalQty(라인 합계)를 주문 수량으로 나눠 상품 1개분을 얻는다.
     */
    List<Map<String, Object>> selectCandidates();

    /** 새 시스템에 이미 있는 규칙 줄(상품명·옵션·순번·코드·수량). */
    List<Map<String, Object>> selectExistingRules();

    /** 레거시 규칙 헤더 중 구성 행(tOrdCurrOrderProd)이 하나도 없어 새 키를 복원할 수 없는 것. */
    List<Map<String, Object>> selectUnmappableHeaders();

    int countLegacyHeaders();

    int countKey(@Param("prodName") String prodName, @Param("itemName") String itemName);
}
