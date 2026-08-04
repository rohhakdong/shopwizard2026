package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderCashRecipt;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderCashReciptMapper {
    List<OrderCashRecipt> selectList(Map<String, Object> params);
    OrderCashRecipt select(Map<String, Object> params);
    void insert(OrderCashRecipt orderCashRecipt);
    void update(OrderCashRecipt orderCashRecipt);
    void delete(Map<String, Object> params);
}
