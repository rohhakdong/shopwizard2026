package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderSoclQuot;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderSoclQuotMapper {
    List<OrderSoclQuot> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderSoclQuot select(Map<String, Object> params);
    int insert(OrderSoclQuot orderSoclQuot);
    int update(OrderSoclQuot orderSoclQuot);
    int delete(OrderSoclQuot orderSoclQuot);
}
