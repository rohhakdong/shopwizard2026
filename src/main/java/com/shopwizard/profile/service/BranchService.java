package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.BranchMapper;
import com.shopwizard.profile.model.Branch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchService {

    private final BranchMapper branchMapper;

    public List<Branch> selectList(Map<String, Object> params) { return branchMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return branchMapper.selectCount(params); }
    public Branch select(Integer branchId) { return branchMapper.select(branchId); }
    public int insert(Branch branch) { return branchMapper.insert(branch); }
    public int update(Branch branch) { return branchMapper.update(branch); }
    public int delete(Integer branchId) { return branchMapper.delete(branchId); }
}
