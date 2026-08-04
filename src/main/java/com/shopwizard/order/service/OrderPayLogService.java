package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderPayLogMapper;
import com.shopwizard.order.model.OrderPayLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderPayLogService {
    private final OrderPayLogMapper orderPayLogMapper;

    public List<OrderPayLog> selectList(Map<String, Object> params) { return orderPayLogMapper.selectList(params); }
    public OrderPayLog select(Integer logId) { return orderPayLogMapper.select(logId); }
}
