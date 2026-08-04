package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CompStoreMapper;
import com.shopwizard.profile.model.CompStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CompStoreService {

    private final CompStoreMapper compStoreMapper;

    public List<CompStore> selectList(Map<String, Object> params) { return compStoreMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return compStoreMapper.selectCount(params); }
    public List<CompStore> selectListFinalTable(Map<String, Object> params) { return compStoreMapper.selectListFinalTable(params); }
    public List<CompStore> selectListLevel(Map<String, Object> params) { return compStoreMapper.selectListLevel(params); }
    public String selectStoreCodeMax(Map<String, Object> params) { return compStoreMapper.selectStoreCodeMax(params); }
    public CompStore select(Map<String, Object> params) { return compStoreMapper.select(params); }
    public int insert(CompStore compStore) { return compStoreMapper.insert(compStore); }
    public int update(CompStore compStore) { return compStoreMapper.update(compStore); }
    public int delete(Map<String, Object> params) { return compStoreMapper.delete(params); }
}
