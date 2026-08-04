package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.DeptStoreMapper;
import com.shopwizard.profile.model.DeptStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DeptStoreService {

    private final DeptStoreMapper deptStoreMapper;

    public List<DeptStore> selectList(Map<String, Object> params) { return deptStoreMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return deptStoreMapper.selectCount(params); }
    public DeptStore select(Map<String, Object> params) { return deptStoreMapper.select(params); }
    public int insert(DeptStore deptStore) { return deptStoreMapper.insert(deptStore); }
    public int update(DeptStore deptStore) { return deptStoreMapper.update(deptStore); }
    public int delete(Map<String, Object> params) { return deptStoreMapper.delete(params); }
}
