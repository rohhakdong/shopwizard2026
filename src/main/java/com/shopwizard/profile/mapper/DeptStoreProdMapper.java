package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.DeptStoreProd;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeptStoreProdMapper {
    List<DeptStoreProd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    DeptStoreProd select(Map<String, Object> params);
    int insert(DeptStoreProd deptStoreProd);
    int update(DeptStoreProd deptStoreProd);
    int delete(Map<String, Object> params);
}
