package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderUpriceMapper;
import com.shopwizard.order.model.OrderUprice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderUpriceService {
    private final OrderUpriceMapper orderUpriceMapper;
    public List<OrderUprice> selectList(Map<String, Object> params) { return orderUpriceMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderUpriceMapper.selectCount(params); }
    public OrderUprice select(Map<String, Object> params) { return orderUpriceMapper.select(params); }
    public int insert(OrderUprice orderUprice) { return orderUpriceMapper.insert(orderUprice); }
    public int update(OrderUprice orderUprice) { return orderUpriceMapper.update(orderUprice); }
    public int delete(OrderUprice orderUprice) { return orderUpriceMapper.delete(orderUprice); }
}
