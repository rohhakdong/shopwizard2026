package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.CustInfoLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustInfoLogMapper {
    List<CustInfoLog> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    CustInfoLog select(Integer logId);
    int insert(CustInfoLog custInfoLog);
    int update(CustInfoLog custInfoLog);
    int delete(Integer logId);
}
