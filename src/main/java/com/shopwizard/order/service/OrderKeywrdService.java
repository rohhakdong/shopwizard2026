package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderKeywrdMapper;
import com.shopwizard.order.model.OrderKeywrd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderKeywrdService {
    private final OrderKeywrdMapper orderKeywrdMapper;
    public List<OrderKeywrd> selectList(Map<String, Object> params) { return orderKeywrdMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderKeywrdMapper.selectCount(params); }
    public OrderKeywrd select(Map<String, Object> params) { return orderKeywrdMapper.select(params); }
    public int insert(OrderKeywrd orderKeywrd) { return orderKeywrdMapper.insert(orderKeywrd); }
    public int update(OrderKeywrd orderKeywrd) { return orderKeywrdMapper.update(orderKeywrd); }
    public int delete(OrderKeywrd orderKeywrd) { return orderKeywrdMapper.delete(orderKeywrd); }
}
