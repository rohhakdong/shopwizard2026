package com.shopwizard.adjust.mapper;

import com.shopwizard.adjust.model.AdjustDetail;
import com.shopwizard.adjust.model.AdjustNoSeq;
import com.shopwizard.adjust.model.AdjustTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 정산 생성·마감 전용 쿼리(AdjustProcessMapper.xml). */
@Mapper
public interface AdjustProcessMapper {

    // ── 정산 대상 조회 ──
    List<AdjustTarget> selectTargetList(Map<String, Object> params);
    int selectTargetCount(Map<String, Object> params);
    /** 구분(kind)별 {kind, cnt, saleAmt, supplyAmt, buyAmt}. */
    List<Map<String, Object>> selectTargetSum(Map<String, Object> params);
    /** 상점별 아직 정산자료로 만들지 않은 대상 건수 {shopCode, cnt}. */
    List<Map<String, Object>> selectOpenTargetCountByShop(Map<String, Object> params);

    // ── 정산자료 생성/취소 ──
    int markShipSelected(Map<String, Object> params);
    int markReturnSelected(Map<String, Object> params);
    int unmarkShipSelected(Map<String, Object> params);
    int unmarkReturnSelected(Map<String, Object> params);
    int insertDetailFromShip(Map<String, Object> params);
    int insertDetailFromReturn(Map<String, Object> params);
    /** 상세 한 건을 잠그고 읽는다(FOR UPDATE). */
    AdjustDetail selectDetailForUpdate(@Param("adjustDetailNo") Integer adjustDetailNo);
    int deleteOpenDetail(@Param("adjustDetailNo") Integer adjustDetailNo);
    int updatePricesOpen(Map<String, Object> params);

    // ── 상점 마감 ──
    /** 상점 행을 잠그고 {shopCode, supplyCode, svcCode, adjustPeriodCode} 를 읽는다. 없으면 null. */
    Map<String, Object> selectShopForClose(@Param("shopCode") String shopCode);
    /** 상점·기간의 미마감 정산자료 건수 — 0 이면 정산번호를 채번하지 않고 끝낸다. */
    int countClosableDetails(@Param("shopCode") String shopCode, @Param("startDate") String startDate, @Param("endDate") String endDate);
    int insertAdjustNo(AdjustNoSeq seq);
}
