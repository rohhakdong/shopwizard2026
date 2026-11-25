package com.shopwizard.order.mapper;

import com.shopwizard.order.model.UpriceMatchDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UpriceMatchMapper {
    /** 지불완료 주문 중 발주단가매칭이 안 된 (상품명,옵션) 그룹 목록. */
    List<Map<String, Object>> selectListUnmatched(Map<String, Object> params);
    int selectListUnmatchedCount(Map<String, Object> params);

    /** (상품명,옵션) 의 매칭 상세 + 발주단가(Uprice) 조인 결과. 빈 리스트 = 미매칭. */
    List<UpriceMatchDetail> selectPriced(@Param("prodName") String prodName, @Param("itemName") String itemName);

    /** 규칙(상품명+옵션)별 목록 — 구성·원가 합계·못 쓰는 코드 수 포함. pProdName/pItemName(LIKE), pPageOffset/pPageSize. */
    List<Map<String, Object>> selectRules(Map<String, Object> params);
    int selectRuleCount(Map<String, Object> params);

    /** 규칙의 줄 — {@link #selectPriced} 와 달리 사용 중지·삭제된 코드도 나온다(편집용). */
    List<UpriceMatchDetail> selectRuleLines(@Param("prodName") String prodName, @Param("itemName") String itemName);

    void deleteByProdItem(@Param("prodName") String prodName, @Param("itemName") String itemName);
    void insertDetail(UpriceMatchDetail detail);
}
