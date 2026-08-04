package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderGiftRule;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderGiftRuleMapper {
    List<OrderGiftRule> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderGiftRule select(Map<String, Object> params);
    int insert(OrderGiftRule orderGiftRule);
    int update(OrderGiftRule orderGiftRule);
    int delete(OrderGiftRule orderGiftRule);
}
