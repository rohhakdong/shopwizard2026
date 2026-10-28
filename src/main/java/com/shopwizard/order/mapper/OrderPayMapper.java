package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderPay;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderPayMapper {
    List<OrderPay> selectList(Map<String, Object> params);
    OrderPay select(Map<String, Object> params);
    OrderPay selectByPgTradeNo(String pgTradeNo);
    /** 같은 PG 의 같은 가맹점 주문번호로 이미 저장된 결제의 주문번호(없으면 null) — 재전송·새로고침 때 중복 주문을 막는다. */
    Integer selectOrderNoByPgOrder(@org.apache.ibatis.annotations.Param("pgCode") String pgCode,
                                   @org.apache.ibatis.annotations.Param("pgOrderCode") String pgOrderCode);
    OrderPay selectPgByOrderNo(int orderNo);
    void insert(OrderPay orderPay);
    void update(OrderPay orderPay);
    void updateOrderPayRecipt(Map<String, Object> params);
    void updateOrderPayReciptAuto(Map<String, Object> params);
    void delete(Map<String, Object> params);
}
