package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.DeptEmp;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeptEmpMapper {
    List<DeptEmp> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    DeptEmp select(Map<String, Object> params);
    int insert(DeptEmp deptEmp);
    int update(DeptEmp deptEmp);
    int delete(Map<String, Object> params);
}
