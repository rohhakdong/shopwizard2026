package com.shopwizard.ship.mapper;

import com.shopwizard.ship.model.ShipPlaceRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ShipPlaceRuleMapper {
    List<ShipPlaceRule> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ShipPlaceRule select(Map<String, Object> params);
    /** 적용용 — 상점의 사용 중(State=1) 규칙을 번호 순으로. */
    List<ShipPlaceRule> selectActiveByShop(@Param("shopCode") String shopCode);
    int insert(ShipPlaceRule rule);
    int update(ShipPlaceRule rule);
    int delete(ShipPlaceRule rule);

    int countShop(@Param("shopCode") String shopCode);
    int countChnl(@Param("chnlCode") String chnlCode);
}
