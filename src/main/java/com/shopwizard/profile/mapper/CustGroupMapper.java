package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.CustGroup;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustGroupMapper {
    List<CustGroup> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    CustGroup select(String custGroupCode);
    int insert(CustGroup custGroup);
    int update(CustGroup custGroup);
    int delete(String custGroupCode);
}
