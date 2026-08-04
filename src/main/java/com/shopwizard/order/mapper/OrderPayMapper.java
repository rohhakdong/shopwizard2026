package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderPay;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderPayMapper {
    List<OrderPay> selectList(Map<String, Object> params);
    OrderPay select(Map<String, Object> params);
    void insert(OrderPay orderPay);
    void update(OrderPay orderPay);
    void updateOrderPayRecipt(Map<String, Object> params);
    void updateOrderPayReciptAuto(Map<String, Object> params);
    void delete(Map<String, Object> params);
}
