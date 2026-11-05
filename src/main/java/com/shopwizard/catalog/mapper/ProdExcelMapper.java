package com.shopwizard.catalog.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 엑셀 일괄등록 검사용 읽기 전용 조회. */
@Mapper
public interface ProdExcelMapper {
    /** 상점의 서비스코드. 없는 상점이면 null. */
    String selectShopSvc(@Param("shopCode") String shopCode);

    /** 서비스에 존재하는 최종 분류(자식이 없는 분류)인지. */
    int countLeafCate(@Param("svcCode") String svcCode, @Param("cateCode") String cateCode);

    /** 등록 쪽(tCatProd)에 있는 상품인지. */
    int countProd(@Param("prodCode") String prodCode);

    /** 판매 상품(shopion.tPrdProd)에 게시돼 있는지 — 수정이 쇼핑몰에 바로 반영되는 상품. */
    int countPublished(@Param("prodCode") String prodCode);

    /** 같은 상점상품코드를 쓰는 다른 상품 수(유니크 키). exceptProdCode 는 수정 대상 자기 자신 제외용. */
    int countShopProdCode(@Param("shopProdCode") String shopProdCode, @Param("exceptProdCode") String exceptProdCode);

    /** tCatProd·tPrdProd 의 상품코드 최댓값(없으면 null) — 신규 코드 채번의 기준. */
    String selectMaxProdCode();

    int countBrand(@Param("svcCode") String svcCode, @Param("name") String name);
    int countMaker(@Param("name") String name);
    int countOrigin(@Param("name") String name);
}
