package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.CompStore;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CompStoreMapper {
    List<CompStore> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    List<CompStore> selectListFinalTable(Map<String, Object> params);
    List<CompStore> selectListLevel(Map<String, Object> params);
    String selectStoreCodeMax(Map<String, Object> params);
    CompStore select(Map<String, Object> params);
    int insert(CompStore compStore);
    int update(CompStore compStore);
    int delete(Map<String, Object> params);
}
