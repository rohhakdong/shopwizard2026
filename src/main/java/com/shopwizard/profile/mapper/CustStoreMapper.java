package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.CustStore;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustStoreMapper {
    List<CustStore> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    CustStore select(Map<String, Object> params);
    int insert(CustStore custStore);
    int update(CustStore custStore);
    int delete(Map<String, Object> params);
}
