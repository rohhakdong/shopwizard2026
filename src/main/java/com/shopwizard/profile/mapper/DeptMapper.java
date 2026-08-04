package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.Dept;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeptMapper {
    List<Dept> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Dept select(Map<String, Object> params);
    Map<String, Object> selectDeptCodeMax(Map<String, Object> params);
    int insert(Dept dept);
    int update(Dept dept);
    int delete(Map<String, Object> params);
}
