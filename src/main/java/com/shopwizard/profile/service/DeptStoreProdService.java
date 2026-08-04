package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.DeptStoreProdMapper;
import com.shopwizard.profile.model.DeptStoreProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DeptStoreProdService {

    private final DeptStoreProdMapper deptStoreProdMapper;

    public List<DeptStoreProd> selectList(Map<String, Object> params) { return deptStoreProdMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return deptStoreProdMapper.selectCount(params); }
    public DeptStoreProd select(Map<String, Object> params) { return deptStoreProdMapper.select(params); }
    public int insert(DeptStoreProd deptStoreProd) { return deptStoreProdMapper.insert(deptStoreProd); }
    public int update(DeptStoreProd deptStoreProd) { return deptStoreProdMapper.update(deptStoreProd); }
    public int delete(Map<String, Object> params) { return deptStoreProdMapper.delete(params); }
}
