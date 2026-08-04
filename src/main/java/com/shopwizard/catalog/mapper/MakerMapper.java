package com.shopwizard.catalog.mapper;

import com.shopwizard.catalog.model.Maker;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface MakerMapper {
    List<Maker> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Maker select(Map<String, Object> params);
    int insert(Maker maker);
    int update(Maker maker);
    int delete(Map<String, Object> params);
}
