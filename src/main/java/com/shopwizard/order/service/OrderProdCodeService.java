package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderProdCodeMapper;
import com.shopwizard.order.model.OrderProdCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderProdCodeService {
    private final OrderProdCodeMapper orderProdCodeMapper;
    public List<OrderProdCode> selectList(Map<String, Object> params) { return orderProdCodeMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderProdCodeMapper.selectCount(params); }
    public OrderProdCode select(Map<String, Object> params) { return orderProdCodeMapper.select(params); }
    public int insert(OrderProdCode orderProdCode) { return orderProdCodeMapper.insert(orderProdCode); }
    public int update(OrderProdCode orderProdCode) { return orderProdCodeMapper.update(orderProdCode); }
    public int delete(OrderProdCode orderProdCode) { return orderProdCodeMapper.delete(orderProdCode); }
}
