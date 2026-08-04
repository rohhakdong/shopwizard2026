package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderCashReciptMapper;
import com.shopwizard.order.model.OrderCashRecipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderCashReciptService {
    private final OrderCashReciptMapper orderCashReciptMapper;

    public List<OrderCashRecipt> selectList(Map<String, Object> params) { return orderCashReciptMapper.selectList(params); }
    public OrderCashRecipt select(Map<String, Object> params) { return orderCashReciptMapper.select(params); }
    public void insert(OrderCashRecipt orderCashRecipt) { orderCashReciptMapper.insert(orderCashRecipt); }
    public void update(OrderCashRecipt orderCashRecipt) { orderCashReciptMapper.update(orderCashRecipt); }
    public void delete(Map<String, Object> params) { orderCashReciptMapper.delete(params); }
}
