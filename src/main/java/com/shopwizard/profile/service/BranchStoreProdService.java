package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.BranchStoreProdMapper;
import com.shopwizard.profile.model.BranchStoreProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchStoreProdService {

    private final BranchStoreProdMapper branchStoreProdMapper;

    public List<BranchStoreProd> selectList(Map<String, Object> params) { return branchStoreProdMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return branchStoreProdMapper.selectCount(params); }
    public BranchStoreProd select(Map<String, Object> params) { return branchStoreProdMapper.select(params); }
    public int insert(BranchStoreProd branchStoreProd) { return branchStoreProdMapper.insert(branchStoreProd); }
    public int update(BranchStoreProd branchStoreProd) { return branchStoreProdMapper.update(branchStoreProd); }
    public int delete(Map<String, Object> params) { return branchStoreProdMapper.delete(params); }
}
