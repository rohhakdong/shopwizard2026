package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderUprice;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderUpriceMapper {
    List<OrderUprice> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderUprice select(Map<String, Object> params);
    int insert(OrderUprice orderUprice);
    int update(OrderUprice orderUprice);
    int delete(OrderUprice orderUprice);
}
