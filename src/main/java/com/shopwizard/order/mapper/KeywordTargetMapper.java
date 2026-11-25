package com.shopwizard.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 키워드로 대상 가져오기 — 상점 미지정 지불완료 라인 조회와 상점 배정. */
@Mapper
public interface KeywordTargetMapper {

    /** 상점 마스터에서 이름(없으면 null) — 상점이 실제로 있는지도 겸한다. */
    String selectShopName(@Param("shopCode") String shopCode);

    /**
     * 상점이 지정되지 않은 지불완료 라인(그 상점과 같은 서비스의 채널, 자체몰 채널 제외). 키워드 판정에 쓰는 최소 컬럼만.
     * 17,000건대라 한 번에 읽어 자바에서 키워드를 판정한다.
     */
    List<Map<String, Object>> selectUnassignedLines(@Param("shopCode") String shopCode, @Param("ownMallChnlCode") String ownMallChnlCode);

    /** 아직 상점이 비어 있는 지불완료 라인에만 상점을 배정한다(좁은 UPDATE). 0행이면 그 사이 바뀐 것. */
    int assignShop(@Param("orderNo") Integer orderNo, @Param("orderProdNo") Integer orderProdNo,
                   @Param("shopCode") String shopCode, @Param("shopName") String shopName,
                   @Param("actorId") String actorId, @Param("actorName") String actorName);
}
