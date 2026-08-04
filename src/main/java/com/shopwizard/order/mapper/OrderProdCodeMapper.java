package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderProdCode;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderProdCodeMapper {
    List<OrderProdCode> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderProdCode select(Map<String, Object> params);
    int insert(OrderProdCode orderProdCode);
    int update(OrderProdCode orderProdCode);
    int delete(OrderProdCode orderProdCode);
}
