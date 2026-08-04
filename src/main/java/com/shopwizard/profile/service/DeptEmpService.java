package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.DeptEmpMapper;
import com.shopwizard.profile.model.DeptEmp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DeptEmpService {

    private final DeptEmpMapper deptEmpMapper;

    public List<DeptEmp> selectList(Map<String, Object> params) { return deptEmpMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return deptEmpMapper.selectCount(params); }
    public DeptEmp select(Map<String, Object> params) { return deptEmpMapper.select(params); }
    public int insert(DeptEmp deptEmp) { return deptEmpMapper.insert(deptEmp); }
    public int update(DeptEmp deptEmp) { return deptEmpMapper.update(deptEmp); }
    public int delete(Map<String, Object> params) { return deptEmpMapper.delete(params); }
}
