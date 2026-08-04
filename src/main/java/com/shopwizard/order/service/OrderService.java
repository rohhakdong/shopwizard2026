package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderMapper orderMapper;

    public List<Order> selectList(Map<String, Object> params) { return orderMapper.selectList(params); }
    public List<Order> selectListOrderList(Map<String, Object> params) { return orderMapper.selectListOrderList(params); }
    public int selectCountOrderList(Map<String, Object> params) { return orderMapper.selectCountOrderList(params); }
    public List<Order> selectListPay(Map<String, Object> params) { return orderMapper.selectListPay(params); }
    public Order select(Map<String, Object> params) { return orderMapper.select(params); }
    public void insert(Order order) { orderMapper.insert(order); }
    public void updateOrderCancel(Order order) { orderMapper.updateOrderCancel(order); }
    public void updateOrderState(Map<String, Object> params) { orderMapper.updateOrderState(params); }
    public void updateRefundCancel(Map<String, Object> params) { orderMapper.updateRefundCancel(params); }
}
