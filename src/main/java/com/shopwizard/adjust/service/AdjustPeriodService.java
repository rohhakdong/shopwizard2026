package com.shopwizard.adjust.service;

import com.shopwizard.adjust.mapper.AdjustPeriodMapper;
import com.shopwizard.adjust.model.AdjustPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AdjustPeriodService {

    private final AdjustPeriodMapper adjustPeriodMapper;

    public List<AdjustPeriod> selectList(Map<String, Object> params) { return adjustPeriodMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return adjustPeriodMapper.selectCount(params); }
    public AdjustPeriod select(Map<String, Object> params) { return adjustPeriodMapper.select(params); }
    public void insert(AdjustPeriod adjustPeriod) { adjustPeriodMapper.insert(adjustPeriod); }
    public void update(AdjustPeriod adjustPeriod) { adjustPeriodMapper.update(adjustPeriod); }
    public void delete(AdjustPeriod adjustPeriod) { adjustPeriodMapper.delete(adjustPeriod); }
}
