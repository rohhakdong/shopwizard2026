package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderUpriceMatchMapper;
import com.shopwizard.order.model.OrderUpriceMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderUpriceMatchService {
    private final OrderUpriceMatchMapper orderUpriceMatchMapper;
    public List<OrderUpriceMatch> selectList(Map<String, Object> params) { return orderUpriceMatchMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderUpriceMatchMapper.selectCount(params); }
    public OrderUpriceMatch select(Map<String, Object> params) { return orderUpriceMatchMapper.select(params); }
    public int insert(OrderUpriceMatch orderUpriceMatch) { return orderUpriceMatchMapper.insert(orderUpriceMatch); }
    public int update(OrderUpriceMatch orderUpriceMatch) { return orderUpriceMatchMapper.update(orderUpriceMatch); }
    public int delete(OrderUpriceMatch orderUpriceMatch) { return orderUpriceMatchMapper.delete(orderUpriceMatch); }
    public int deleteByOrderNo(Map<String, Object> params) { return orderUpriceMatchMapper.deleteByOrderNo(params); }
}
