package com.shopwizard.authority.service;

import com.shopwizard.authority.mapper.SvcMapper;
import com.shopwizard.authority.model.Svc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SvcService {

    private final SvcMapper svcMapper;

    public List<Svc> selectList(Map<String, Object> params) {
        return svcMapper.selectList(params);
    }

    public int selectCount(Map<String, Object> params) {
        return svcMapper.selectCount(params);
    }

    public Svc select(Map<String, Object> params) {
        return svcMapper.select(params);
    }

    @Transactional
    public int insert(Svc svc) {
        return svcMapper.insert(svc);
    }

    @Transactional
    public int update(Svc svc) {
        return svcMapper.update(svc);
    }

    @Transactional
    public int delete(Svc svc) {
        return svcMapper.delete(svc);
    }
}
