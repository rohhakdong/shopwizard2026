package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderUpriceOptionMatchMapper;
import com.shopwizard.order.model.OrderUpriceOptionMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderUpriceOptionMatchService {
    private final OrderUpriceOptionMatchMapper orderUpriceOptionMatchMapper;
    public List<OrderUpriceOptionMatch> selectList(Map<String, Object> params) { return orderUpriceOptionMatchMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderUpriceOptionMatchMapper.selectCount(params); }
    public OrderUpriceOptionMatch select(Map<String, Object> params) { return orderUpriceOptionMatchMapper.select(params); }
    public int insert(OrderUpriceOptionMatch orderUpriceOptionMatch) { return orderUpriceOptionMatchMapper.insert(orderUpriceOptionMatch); }
    public int update(OrderUpriceOptionMatch orderUpriceOptionMatch) { return orderUpriceOptionMatchMapper.update(orderUpriceOptionMatch); }
    public int delete(OrderUpriceOptionMatch orderUpriceOptionMatch) { return orderUpriceOptionMatchMapper.delete(orderUpriceOptionMatch); }
    public int deleteByOrderNo(Map<String, Object> params) { return orderUpriceOptionMatchMapper.deleteByOrderNo(params); }
}
