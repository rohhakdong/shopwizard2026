package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustStoreProdMapper;
import com.shopwizard.profile.model.CustStoreProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustStoreProdService {

    private final CustStoreProdMapper custStoreProdMapper;

    public List<CustStoreProd> selectList(Map<String, Object> params) { return custStoreProdMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custStoreProdMapper.selectCount(params); }
    public CustStoreProd select(Map<String, Object> params) { return custStoreProdMapper.select(params); }
    public int insert(CustStoreProd custStoreProd) { return custStoreProdMapper.insert(custStoreProd); }
    public int update(CustStoreProd custStoreProd) { return custStoreProdMapper.update(custStoreProd); }
    public int delete(Map<String, Object> params) { return custStoreProdMapper.delete(params); }
}
