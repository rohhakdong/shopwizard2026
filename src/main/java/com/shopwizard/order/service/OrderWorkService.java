package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderWorkMapper;
import com.shopwizard.order.model.OrderWork;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderWorkService {
    private final OrderWorkMapper orderWorkMapper;
    public List<OrderWork> selectList(Map<String, Object> params) { return orderWorkMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderWorkMapper.selectCount(params); }
    public OrderWork select(Map<String, Object> params) { return orderWorkMapper.select(params); }
    public int insert(OrderWork orderWork) { return orderWorkMapper.insert(orderWork); }
    public int update(OrderWork orderWork) { return orderWorkMapper.update(orderWork); }
    public int delete(OrderWork orderWork) { return orderWorkMapper.delete(orderWork); }
}
