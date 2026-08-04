package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderUpriceMatch;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderUpriceMatchMapper {
    List<OrderUpriceMatch> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderUpriceMatch select(Map<String, Object> params);
    int insert(OrderUpriceMatch orderUpriceMatch);
    int update(OrderUpriceMatch orderUpriceMatch);
    int delete(OrderUpriceMatch orderUpriceMatch);
    int deleteByOrderNo(Map<String, Object> params);
}
