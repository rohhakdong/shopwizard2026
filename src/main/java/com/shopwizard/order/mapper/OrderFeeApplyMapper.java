package com.shopwizard.order.mapper;

import com.shopwizard.order.model.FeeLine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 수수료 규칙 적용 전용 조회 — 상점의 지불완료 주문 라인. */
@Mapper
public interface OrderFeeApplyMapper {
    List<FeeLine> selectPayCompleteLines(@Param("shopCode") String shopCode);
}
