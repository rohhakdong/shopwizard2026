package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderQuotDetailMapper;
import com.shopwizard.order.model.OrderQuotDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderQuotDetailService {
    private final OrderQuotDetailMapper orderQuotDetailMapper;
    public List<OrderQuotDetail> selectList(Map<String, Object> params) { return orderQuotDetailMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return orderQuotDetailMapper.selectCount(params); }
    public OrderQuotDetail select(Map<String, Object> params) { return orderQuotDetailMapper.select(params); }
    public int selectMax(Map<String, Object> params) { return orderQuotDetailMapper.selectMax(params); }
    public int insert(OrderQuotDetail orderQuotDetail) { return orderQuotDetailMapper.insert(orderQuotDetail); }
    public int update(OrderQuotDetail orderQuotDetail) { return orderQuotDetailMapper.update(orderQuotDetail); }
    public int delete(OrderQuotDetail orderQuotDetail) { return orderQuotDetailMapper.delete(orderQuotDetail); }
    public int deleteByQuotNo(Map<String, Object> params) { return orderQuotDetailMapper.deleteByQuotNo(params); }
}
