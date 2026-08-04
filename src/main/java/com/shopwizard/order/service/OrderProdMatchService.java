package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderProdMatchMapper;
import com.shopwizard.order.model.OrderProdMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderProdMatchService {
    private final OrderProdMatchMapper orderProdMatchMapper;
    public List<OrderProdMatch> selectList(Map<String, Object> params) { return orderProdMatchMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderProdMatchMapper.selectCount(params); }
    public OrderProdMatch select(Map<String, Object> params) { return orderProdMatchMapper.select(params); }
    public int insert(OrderProdMatch orderProdMatch) { return orderProdMatchMapper.insert(orderProdMatch); }
    public int update(OrderProdMatch orderProdMatch) { return orderProdMatchMapper.update(orderProdMatch); }
    public int delete(OrderProdMatch orderProdMatch) { return orderProdMatchMapper.delete(orderProdMatch); }
    public int deleteByMatchProdCode(Map<String, Object> params) { return orderProdMatchMapper.deleteByMatchProdCode(params); }
}
