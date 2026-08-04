package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderGiftRuleMapper;
import com.shopwizard.order.model.OrderGiftRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderGiftRuleService {
    private final OrderGiftRuleMapper orderGiftRuleMapper;
    public List<OrderGiftRule> selectList(Map<String, Object> params) { return orderGiftRuleMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderGiftRuleMapper.selectCount(params); }
    public OrderGiftRule select(Map<String, Object> params) { return orderGiftRuleMapper.select(params); }
    public int insert(OrderGiftRule orderGiftRule) { return orderGiftRuleMapper.insert(orderGiftRule); }
    public int update(OrderGiftRule orderGiftRule) { return orderGiftRuleMapper.update(orderGiftRule); }
    public int delete(OrderGiftRule orderGiftRule) { return orderGiftRuleMapper.delete(orderGiftRule); }
}
