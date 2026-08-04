package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderFeeRuleMapper;
import com.shopwizard.order.model.OrderFeeRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderFeeRuleService {
    private final OrderFeeRuleMapper orderFeeRuleMapper;
    public List<OrderFeeRule> selectList(Map<String, Object> params) { return orderFeeRuleMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderFeeRuleMapper.selectCount(params); }
    public OrderFeeRule select(Map<String, Object> params) { return orderFeeRuleMapper.select(params); }
    public int insert(OrderFeeRule orderFeeRule) { return orderFeeRuleMapper.insert(orderFeeRule); }
    public int update(OrderFeeRule orderFeeRule) { return orderFeeRuleMapper.update(orderFeeRule); }
    public int delete(OrderFeeRule orderFeeRule) { return orderFeeRuleMapper.delete(orderFeeRule); }
}
