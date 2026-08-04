package com.shopwizard.product.service;

import com.shopwizard.product.mapper.SlinkLogMapper;
import com.shopwizard.product.model.SlinkLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SlinkLogService {

    private final SlinkLogMapper slinkLogMapper;

    public List<SlinkLog> selectList(Map<String, Object> params) { return slinkLogMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return slinkLogMapper.selectCount(params); }
    public SlinkLog select(Map<String, Object> params) { return slinkLogMapper.select(params); }
    public void insert(SlinkLog slinkLog) { slinkLogMapper.insert(slinkLog); }
    public void update(SlinkLog slinkLog) { slinkLogMapper.update(slinkLog); }
    public void delete(SlinkLog slinkLog) { slinkLogMapper.delete(slinkLog); }
}
