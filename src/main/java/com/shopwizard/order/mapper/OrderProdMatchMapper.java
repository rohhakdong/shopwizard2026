package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderProdMatch;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderProdMatchMapper {
    List<OrderProdMatch> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    OrderProdMatch select(Map<String, Object> params);
    int insert(OrderProdMatch orderProdMatch);
    int update(OrderProdMatch orderProdMatch);
    int delete(OrderProdMatch orderProdMatch);
    int deleteByMatchProdCode(Map<String, Object> params);
}
