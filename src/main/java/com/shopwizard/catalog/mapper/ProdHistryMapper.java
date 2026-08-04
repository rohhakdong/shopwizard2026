package com.shopwizard.catalog.mapper;

import com.shopwizard.catalog.model.ProdHistry;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProdHistryMapper {
    List<ProdHistry> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ProdHistry select(Map<String, Object> params);
    int insert(ProdHistry prodHistry);
    int update(ProdHistry prodHistry);
    int delete(ProdHistry prodHistry);
}
