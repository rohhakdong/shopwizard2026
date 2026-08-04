package com.shopwizard.catalog.mapper;

import com.shopwizard.catalog.model.ProdItemHistry;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProdItemHistryMapper {
    List<ProdItemHistry> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ProdItemHistry select(Map<String, Object> params);
    int insert(ProdItemHistry prodItemHistry);
    int update(ProdItemHistry prodItemHistry);
    int delete(ProdItemHistry prodItemHistry);
}
