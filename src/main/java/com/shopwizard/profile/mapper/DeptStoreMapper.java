package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.DeptStore;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeptStoreMapper {
    List<DeptStore> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    DeptStore select(Map<String, Object> params);
    int insert(DeptStore deptStore);
    int update(DeptStore deptStore);
    int delete(Map<String, Object> params);
}
