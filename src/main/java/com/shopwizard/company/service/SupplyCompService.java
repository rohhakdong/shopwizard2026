package com.shopwizard.company.service;

import com.shopwizard.company.mapper.SupplyCompMapper;
import com.shopwizard.company.model.SupplyComp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupplyCompService {
    private final SupplyCompMapper supplyCompMapper;

    public List<SupplyComp> getList(Map<String, Object> params) { return supplyCompMapper.selectList(params); }
    public int getCount(Map<String, Object> params) { return supplyCompMapper.selectCount(params); }
    public SupplyComp get(Map<String, Object> params) { return supplyCompMapper.select(params); }
    public String getMax(Map<String, Object> params) { return supplyCompMapper.selectMax(params); }

    @Transactional
    public int insert(SupplyComp supplyComp) { return supplyCompMapper.insert(supplyComp); }
    @Transactional
    public int update(SupplyComp supplyComp) { return supplyCompMapper.update(supplyComp); }
    @Transactional
    public int delete(Map<String, Object> params) { return supplyCompMapper.delete(params); }
}
