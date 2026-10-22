package com.shopwizard.framework.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 행 소유권 확인용 COUNT 쿼리 — shopCode/chnlCode/svcCode 가 null 인 차원은 조건에서 빠진다. */
@Mapper
public interface DataScopeMapper {
    int countLines(@Param("orderNo") Integer orderNo, @Param("orderProdNo") Integer orderProdNo,
                   @Param("shopCode") String shopCode, @Param("chnlCode") String chnlCode, @Param("svcCode") String svcCode);

    int countOrder(@Param("orderNo") Integer orderNo,
                   @Param("shopCode") String shopCode, @Param("chnlCode") String chnlCode, @Param("svcCode") String svcCode);

    int countCatProd(@Param("prodCode") String prodCode, @Param("shopCode") String shopCode, @Param("svcCode") String svcCode);

    int countPrdProd(@Param("prodCode") String prodCode, @Param("shopCode") String shopCode, @Param("svcCode") String svcCode);

    int countAdjust(@Param("adjustNo") String adjustNo,
                    @Param("shopCode") String shopCode, @Param("chnlCode") String chnlCode, @Param("svcCode") String svcCode);

    // ── 서비스(테넌트) 소유 확인: 그 행이 해당 서비스 것인가(0 이면 없거나 다른 서비스) ──
    int countShopInSvc(@Param("code") String shopCode, @Param("svcCode") String svcCode);
    int countChnlInSvc(@Param("code") String chnlCode, @Param("svcCode") String svcCode);
    int countCompInSvc(@Param("code") String compCode, @Param("svcCode") String svcCode);
    int countSupplyInSvc(@Param("code") String supplyCode, @Param("svcCode") String svcCode);
    int countSaleCompInSvc(@Param("code") String saleCompCode, @Param("svcCode") String svcCode);
    int countWarehsInSvc(@Param("code") String warehsCode, @Param("svcCode") String svcCode);
    int countBrandInSvc(@Param("id") Integer brandId, @Param("svcCode") String svcCode);
    int countRoleInSvc(@Param("code") String roleUid, @Param("svcCode") String svcCode);
    /** 관리자 계정: 그 계정의 역할이 해당 서비스 것인가. */
    int countMngrInSvc(@Param("code") String mngrUid, @Param("svcCode") String svcCode);
}
