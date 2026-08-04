package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.CompStoreProd;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CompStoreProdMapper {
    List<CompStoreProd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    CompStoreProd select(Map<String, Object> params);
    int insert(CompStoreProd compStoreProd);
    int update(CompStoreProd compStoreProd);
    int delete(Map<String, Object> params);
}
