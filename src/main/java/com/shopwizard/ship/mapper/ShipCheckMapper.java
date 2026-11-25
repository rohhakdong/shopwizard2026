package com.shopwizard.ship.mapper;

import com.shopwizard.ship.model.ShipCheck;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ShipCheckMapper {
    List<ShipCheck> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ShipCheck select(Map<String, Object> params);
    int insert(ShipCheck shipCheck);
    int updatePrintCount(ShipCheck shipCheck);
    /** 한 번의 발주확인(같은 ShipCheckDate)에 들어간 상점별 주문 수 — 발주 목록 헤더를 만드는 데 쓴다. */
    List<Map<String, Object>> selectBatchSummary(Map<String, Object> params);
    int delete(ShipCheck shipCheck);
}
