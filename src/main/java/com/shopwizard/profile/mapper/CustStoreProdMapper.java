package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.CustStoreProd;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustStoreProdMapper {
    List<CustStoreProd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    CustStoreProd select(Map<String, Object> params);
    int insert(CustStoreProd custStoreProd);
    int update(CustStoreProd custStoreProd);
    int delete(Map<String, Object> params);
}
