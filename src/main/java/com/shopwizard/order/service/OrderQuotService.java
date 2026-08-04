package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderQuotMapper;
import com.shopwizard.order.model.OrderQuot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderQuotService {
    private final OrderQuotMapper orderQuotMapper;
    public List<OrderQuot> selectList(Map<String, Object> params) { return orderQuotMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderQuotMapper.selectCount(params); }
    public OrderQuot select(Map<String, Object> params) { return orderQuotMapper.select(params); }
    public OrderQuot selectByQuotNo(Map<String, Object> params) { return orderQuotMapper.selectByQuotNo(params); }
    public int insert(OrderQuot orderQuot) { return orderQuotMapper.insert(orderQuot); }
    public int update(OrderQuot orderQuot) { return orderQuotMapper.update(orderQuot); }
    public int delete(OrderQuot orderQuot) { return orderQuotMapper.delete(orderQuot); }
}
