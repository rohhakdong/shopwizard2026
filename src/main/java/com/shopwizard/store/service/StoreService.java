package com.shopwizard.store.service;

import com.shopwizard.store.mapper.StoreMapper;
import com.shopwizard.store.model.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreService {
    private final StoreMapper storeMapper;

    public List<Store> selectList(Map<String, Object> params) { return storeMapper.selectList(params); }
    public String selectStoreCodeMax(Map<String, Object> params) { return storeMapper.selectStoreCodeMax(params); }
    public List<Store> selectListTable(Map<String, Object> params) { return storeMapper.selectListTable(params); }
    public Store select(Map<String, Object> params) { return storeMapper.select(params); }
    public List<Store> selectListLevel(Map<String, Object> params) { return storeMapper.selectListLevel(params); }
    public int insert(Store store) { return storeMapper.insert(store); }
    public int update(Store store) { return storeMapper.update(store); }
    public int delete(Map<String, Object> params) { return storeMapper.delete(params); }
}
