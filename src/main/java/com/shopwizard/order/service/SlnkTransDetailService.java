package com.shopwizard.order.service;

import com.shopwizard.order.mapper.SlnkTransDetailMapper;
import com.shopwizard.order.model.SlnkTransDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SlnkTransDetailService {
    private final SlnkTransDetailMapper slnkTransDetailMapper;

    public List<SlnkTransDetail> selectList(Map<String, Object> params) { return slnkTransDetailMapper.selectList(params); }
    public SlnkTransDetail select(Map<String, Object> params) { return slnkTransDetailMapper.select(params); }
    public void insert(SlnkTransDetail slnkTransDetail) { slnkTransDetailMapper.insert(slnkTransDetail); }
    public void update(SlnkTransDetail slnkTransDetail) { slnkTransDetailMapper.update(slnkTransDetail); }
    public void delete(Map<String, Object> params) { slnkTransDetailMapper.delete(params); }
}
