package com.shopwizard.catalog.mapper;

import com.shopwizard.product.model.SlinkLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 샵링커 상품 전송에 필요한 조회. */
@Mapper
public interface SlinkProdMapper {
    /** 판매 상품(shopion.tPrdProd)의 현재 재고 — 주문으로 차감되는 실재고라 샵링커에는 이 값을 보낸다. 없으면 null. */
    Integer selectLiveSupplyQty(@Param("prodCode") String prodCode);

    /** 상품의 샵링커 전송 이력(최신순). 큰 CallUrl 은 읽지 않는다. */
    List<SlinkLog> selectProdLogs(@Param("prodCode") String prodCode, @Param("limit") int limit);
}
