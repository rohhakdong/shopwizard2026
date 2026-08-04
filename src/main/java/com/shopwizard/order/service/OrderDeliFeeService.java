package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderDeliFeeMapper;
import com.shopwizard.order.model.OrderDeliFee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderDeliFeeService {
    private final OrderDeliFeeMapper orderDeliFeeMapper;

    public List<OrderDeliFee> selectList(Map<String, Object> params) { return orderDeliFeeMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderDeliFeeMapper.selectCount(params); }
    public OrderDeliFee select(Map<String, Object> params) { return orderDeliFeeMapper.select(params); }
    public void insert(OrderDeliFee orderDeliFee) { orderDeliFeeMapper.insert(orderDeliFee); }
    public void update(OrderDeliFee orderDeliFee) { orderDeliFeeMapper.update(orderDeliFee); }
    public void delete(Map<String, Object> params) { orderDeliFeeMapper.delete(params); }
}
