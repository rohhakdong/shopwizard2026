package com.shopwizard.ship.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 발주내역·발주내역2 조회(읽기 전용) — 발주서 테이블 tShpShipDirectPrint. */
@Mapper
public interface ShipPrintMapper {

    /** 발주내역(30열). startDate/endDate 는 둘 다 있을 때만 발주일 범위로 건다. offset/limit 이 null 이면 전체. */
    List<Map<String, Object>> selectList(@Param("shopCode") String shopCode, @Param("svcCode") String svcCode,
                                         @Param("startDate") String startDate, @Param("endDate") String endDate,
                                         @Param("offset") Integer offset, @Param("limit") Integer limit);

    int selectCount(@Param("shopCode") String shopCode, @Param("svcCode") String svcCode,
                    @Param("startDate") String startDate, @Param("endDate") String endDate);

    /** 발주내역2(28열, 사은품 행 제외). dateType: S=발주일, O=주문일. */
    List<Map<String, Object>> selectList2(@Param("shopCode") String shopCode, @Param("svcCode") String svcCode,
                                          @Param("dateType") String dateType,
                                          @Param("startDate") String startDate, @Param("endDate") String endDate,
                                          @Param("chnlCode") String chnlCode, @Param("mdCode") String mdCode,
                                          @Param("prodName") String prodName,
                                          @Param("offset") Integer offset, @Param("limit") Integer limit);

    int selectCount2(@Param("shopCode") String shopCode, @Param("svcCode") String svcCode,
                     @Param("dateType") String dateType,
                     @Param("startDate") String startDate, @Param("endDate") String endDate,
                     @Param("chnlCode") String chnlCode, @Param("mdCode") String mdCode,
                     @Param("prodName") String prodName);

    String selectShopName(@Param("shopCode") String shopCode);
}
