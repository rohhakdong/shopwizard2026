package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustStoreMapper;
import com.shopwizard.profile.model.CustStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustStoreService {

    private final CustStoreMapper custStoreMapper;

    public List<CustStore> selectList(Map<String, Object> params) { return custStoreMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custStoreMapper.selectCount(params); }
    public CustStore select(Map<String, Object> params) { return custStoreMapper.select(params); }
    public int insert(CustStore custStore) { return custStoreMapper.insert(custStore); }
    public int update(CustStore custStore) { return custStoreMapper.update(custStore); }
    public int delete(Map<String, Object> params) { return custStoreMapper.delete(params); }
}
