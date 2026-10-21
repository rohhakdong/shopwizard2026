package com.shopwizard.framework.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 행 소유권 확인용 COUNT 쿼리 — shopCode/chnlCode 가 null 인 차원은 조건에서 빠진다. */
@Mapper
public interface DataScopeMapper {
    int countLines(@Param("orderNo") Integer orderNo, @Param("orderProdNo") Integer orderProdNo,
                   @Param("shopCode") String shopCode, @Param("chnlCode") String chnlCode);

    int countOrder(@Param("orderNo") Integer orderNo,
                   @Param("shopCode") String shopCode, @Param("chnlCode") String chnlCode);

    int countCatProd(@Param("prodCode") String prodCode, @Param("shopCode") String shopCode);

    int countPrdProd(@Param("prodCode") String prodCode, @Param("shopCode") String shopCode);

    int countAdjust(@Param("adjustNo") String adjustNo,
                    @Param("shopCode") String shopCode, @Param("chnlCode") String chnlCode);
}
