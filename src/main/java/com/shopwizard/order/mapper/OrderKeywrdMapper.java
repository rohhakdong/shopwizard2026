package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderKeywrd;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderKeywrdMapper {
    List<OrderKeywrd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderKeywrd select(Map<String, Object> params);
    int insert(OrderKeywrd orderKeywrd);
    int update(OrderKeywrd orderKeywrd);
    int delete(OrderKeywrd orderKeywrd);
}
