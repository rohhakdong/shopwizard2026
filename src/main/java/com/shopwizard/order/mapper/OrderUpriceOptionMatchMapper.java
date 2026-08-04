package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderUpriceOptionMatch;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderUpriceOptionMatchMapper {
    List<OrderUpriceOptionMatch> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderUpriceOptionMatch select(Map<String, Object> params);
    int insert(OrderUpriceOptionMatch orderUpriceOptionMatch);
    int update(OrderUpriceOptionMatch orderUpriceOptionMatch);
    int delete(OrderUpriceOptionMatch orderUpriceOptionMatch);
    int deleteByOrderNo(Map<String, Object> params);
}
