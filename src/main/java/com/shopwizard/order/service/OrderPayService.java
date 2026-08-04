package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderPayMapper;
import com.shopwizard.order.model.OrderPay;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderPayService {
    private final OrderPayMapper orderPayMapper;

    public List<OrderPay> selectList(Map<String, Object> params) { return orderPayMapper.selectList(params); }
    public OrderPay select(Map<String, Object> params) { return orderPayMapper.select(params); }
    public void insert(OrderPay orderPay) { orderPayMapper.insert(orderPay); }
    public void update(OrderPay orderPay) { orderPayMapper.update(orderPay); }
    public void updateOrderPayRecipt(Map<String, Object> params) { orderPayMapper.updateOrderPayRecipt(params); }
    public void updateOrderPayReciptAuto(Map<String, Object> params) { orderPayMapper.updateOrderPayReciptAuto(params); }
    public void delete(Map<String, Object> params) { orderPayMapper.delete(params); }
}
