package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderChangeNoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderChangeNoService {
    private final OrderChangeNoMapper orderChangeNoMapper;

    public int selectCount(Map<String, Object> params) { return orderChangeNoMapper.selectCount(params); }
    public int selectMax(Map<String, Object> params) { return orderChangeNoMapper.selectMax(params); }
    public Integer selectChangeNo(Map<String, Object> params) { return orderChangeNoMapper.selectChangeNo(params); }
    public void insert(Map<String, Object> params) { orderChangeNoMapper.insert(params); }
}
