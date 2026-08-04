package com.shopwizard.store.service;

import com.shopwizard.store.mapper.StoreProdMapper;
import com.shopwizard.store.model.StoreProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreProdService {
    private final StoreProdMapper storeProdMapper;

    public List<StoreProd> selectList(Map<String, Object> params) { return storeProdMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return storeProdMapper.selectCount(params); }
    public List<StoreProd> selectListLocType(Map<String, Object> params) { return storeProdMapper.selectListLocType(params); }
    public List<StoreProd> selectListOrderQty(Map<String, Object> params) { return storeProdMapper.selectListOrderQty(params); }
    public String selectStoreCode(Map<String, Object> params) { return storeProdMapper.selectStoreCode(params); }
    public StoreProd select(Map<String, Object> params) { return storeProdMapper.select(params); }
    public int insert(StoreProd storeProd) { return storeProdMapper.insert(storeProd); }
    public int update(StoreProd storeProd) { return storeProdMapper.update(storeProd); }
    public int delete(Map<String, Object> params) { return storeProdMapper.delete(params); }
    public int deleteProd(Map<String, Object> params) { return storeProdMapper.deleteProd(params); }
}
