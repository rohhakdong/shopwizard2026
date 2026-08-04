package com.shopwizard.catalog.mapper;

import com.shopwizard.catalog.model.Origin;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OriginMapper {
    List<Origin> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Origin select(Map<String, Object> params);
    int insert(Origin origin);
    int update(Origin origin);
    int delete(Map<String, Object> params);
}
