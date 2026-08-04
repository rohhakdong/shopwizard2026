package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderPayLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderPayLogMapper {
    List<OrderPayLog> selectList(Map<String, Object> params);
    OrderPayLog select(Integer logId);
}
