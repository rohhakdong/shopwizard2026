package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.DeptMapper;
import com.shopwizard.profile.model.Dept;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DeptService {

    private final DeptMapper deptMapper;

    public List<Dept> selectList(Map<String, Object> params) { return deptMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return deptMapper.selectCount(params); }
    public Dept select(Map<String, Object> params) { return deptMapper.select(params); }
    public Map<String, Object> selectDeptCodeMax(Map<String, Object> params) { return deptMapper.selectDeptCodeMax(params); }
    public int insert(Dept dept) { return deptMapper.insert(dept); }
    public int update(Dept dept) { return deptMapper.update(dept); }
    public int delete(Map<String, Object> params) { return deptMapper.delete(params); }
}
