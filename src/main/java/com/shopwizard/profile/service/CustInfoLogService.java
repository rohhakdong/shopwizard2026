package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustInfoLogMapper;
import com.shopwizard.profile.model.CustInfoLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustInfoLogService {

    private final CustInfoLogMapper custInfoLogMapper;

    public List<CustInfoLog> selectList(Map<String, Object> params) { return custInfoLogMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custInfoLogMapper.selectCount(params); }
    public CustInfoLog select(Integer logId) { return custInfoLogMapper.select(logId); }
}
