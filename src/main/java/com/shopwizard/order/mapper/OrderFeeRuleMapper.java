package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderFeeRule;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderFeeRuleMapper {
    List<OrderFeeRule> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderFeeRule select(Map<String, Object> params);
    int insert(OrderFeeRule orderFeeRule);
    int update(OrderFeeRule orderFeeRule);
    int delete(OrderFeeRule orderFeeRule);
}
