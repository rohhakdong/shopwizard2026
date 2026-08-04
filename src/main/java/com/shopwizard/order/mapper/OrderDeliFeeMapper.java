package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderDeliFee;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderDeliFeeMapper {
    List<OrderDeliFee> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderDeliFee select(Map<String, Object> params);
    void insert(OrderDeliFee orderDeliFee);
    void update(OrderDeliFee orderDeliFee);
    void delete(Map<String, Object> params);
}
