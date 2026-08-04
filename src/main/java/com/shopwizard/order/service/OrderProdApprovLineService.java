package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderProdApprovLineMapper;
import com.shopwizard.order.model.OrderProdApprovLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderProdApprovLineService {
    private final OrderProdApprovLineMapper orderProdApprovLineMapper;

    public List<OrderProdApprovLine> selectList(Map<String, Object> params) { return orderProdApprovLineMapper.selectList(params); }
    public int count(Map<String, Object> params) { return orderProdApprovLineMapper.count(params); }
    public int countReady(Map<String, Object> params) { return orderProdApprovLineMapper.countReady(params); }
    public int countApprov(Map<String, Object> params) { return orderProdApprovLineMapper.countApprov(params); }
    public int countReject(Map<String, Object> params) { return orderProdApprovLineMapper.countReject(params); }
    public void insert(Map<String, Object> params) { orderProdApprovLineMapper.insert(params); }
    public void updateApprov(Map<String, Object> params) { orderProdApprovLineMapper.updateApprov(params); }
    public void updateReject(Map<String, Object> params) { orderProdApprovLineMapper.updateReject(params); }
    public void delete(Map<String, Object> params) { orderProdApprovLineMapper.delete(params); }
}
