package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderWork;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderWorkMapper {
    List<OrderWork> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderWork select(Map<String, Object> params);
    int insert(OrderWork orderWork);
    int update(OrderWork orderWork);
    int delete(OrderWork orderWork);
}
