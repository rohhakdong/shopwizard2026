package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderQuot;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderQuotMapper {
    List<OrderQuot> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderQuot select(Map<String, Object> params);
    OrderQuot selectByQuotNo(Map<String, Object> params);
    int insert(OrderQuot orderQuot);
    int update(OrderQuot orderQuot);
    int delete(OrderQuot orderQuot);
}
