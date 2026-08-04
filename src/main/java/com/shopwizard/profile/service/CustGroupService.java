package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustGroupMapper;
import com.shopwizard.profile.model.CustGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustGroupService {

    private final CustGroupMapper custGroupMapper;

    public List<CustGroup> selectList(Map<String, Object> params) { return custGroupMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custGroupMapper.selectCount(params); }
    public CustGroup select(String custGroupCode) { return custGroupMapper.select(custGroupCode); }
    public int insert(CustGroup custGroup) { return custGroupMapper.insert(custGroup); }
    public int update(CustGroup custGroup) { return custGroupMapper.update(custGroup); }
    public int delete(String custGroupCode) { return custGroupMapper.delete(custGroupCode); }
}
