package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.BranchStoreMapper;
import com.shopwizard.profile.model.BranchStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchStoreService {

    private final BranchStoreMapper branchStoreMapper;

    public List<BranchStore> selectList(Map<String, Object> params) { return branchStoreMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return branchStoreMapper.selectCount(params); }
    public BranchStore select(Map<String, Object> params) { return branchStoreMapper.select(params); }
    public int insert(BranchStore branchStore) { return branchStoreMapper.insert(branchStore); }
    public int update(BranchStore branchStore) { return branchStoreMapper.update(branchStore); }
    public int delete(Map<String, Object> params) { return branchStoreMapper.delete(params); }
}
