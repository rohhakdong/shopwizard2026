package com.shopwizard.order.mapper;

import com.shopwizard.order.model.Order;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    List<Order> selectList(Map<String, Object> params);
    List<Order> selectListOrderList(Map<String, Object> params);
    int selectCountOrderList(Map<String, Object> params);
    List<Order> selectListPay(Map<String, Object> params);
    Order select(Map<String, Object> params);
    /** 주문 헤더 행 잠금(FOR UPDATE). 같은 주문의 라인 전이/헤더 승격을 직렬화한다. 주문번호만 돌려준다. */
    Integer selectHeaderForUpdate(@org.apache.ibatis.annotations.Param("orderNo") Integer orderNo);
    void insert(Order order);
    void updateOrderCancel(Order order);
    void updateOrderState(Map<String, Object> params);
    void updateRefundCancel(Map<String, Object> params);
}
