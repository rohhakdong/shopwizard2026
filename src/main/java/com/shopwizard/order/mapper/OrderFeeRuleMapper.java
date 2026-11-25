package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderFeeRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderFeeRuleMapper {
    List<OrderFeeRule> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderFeeRule select(Map<String, Object> params);
    /** 적용용 — 상점의 모든 규칙을 번호 순으로(레거시처럼 State 를 보지 않는다). */
    List<OrderFeeRule> selectByShop(@Param("shopCode") String shopCode);
    int insert(OrderFeeRule orderFeeRule);
    int update(OrderFeeRule orderFeeRule);
    int delete(OrderFeeRule orderFeeRule);

    int countShop(@Param("shopCode") String shopCode);
    int countChnl(@Param("chnlCode") String chnlCode);
}
