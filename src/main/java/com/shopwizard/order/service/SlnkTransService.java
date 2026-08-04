package com.shopwizard.order.service;

import com.shopwizard.order.mapper.SlnkTransMapper;
import com.shopwizard.order.model.SlnkTrans;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SlnkTransService {
    private final SlnkTransMapper slnkTransMapper;

    public List<SlnkTrans> selectList(Map<String, Object> params) { return slnkTransMapper.selectList(params); }
    public SlnkTrans select(Map<String, Object> params) { return slnkTransMapper.select(params); }
    public void insert(SlnkTrans slnkTrans) { slnkTransMapper.insert(slnkTrans); }
    public void update(SlnkTrans slnkTrans) { slnkTransMapper.update(slnkTrans); }
    public void delete(Map<String, Object> params) { slnkTransMapper.delete(params); }
}
