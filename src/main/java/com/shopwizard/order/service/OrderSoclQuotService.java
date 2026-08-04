package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderSoclQuotMapper;
import com.shopwizard.order.model.OrderSoclQuot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderSoclQuotService {
    private final OrderSoclQuotMapper orderSoclQuotMapper;
    public List<OrderSoclQuot> selectList(Map<String, Object> params) { return orderSoclQuotMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderSoclQuotMapper.selectCount(params); }
    public OrderSoclQuot select(Map<String, Object> params) { return orderSoclQuotMapper.select(params); }
    public int insert(OrderSoclQuot orderSoclQuot) { return orderSoclQuotMapper.insert(orderSoclQuot); }
    public int update(OrderSoclQuot orderSoclQuot) { return orderSoclQuotMapper.update(orderSoclQuot); }
    public int delete(OrderSoclQuot orderSoclQuot) { return orderSoclQuotMapper.delete(orderSoclQuot); }
}
