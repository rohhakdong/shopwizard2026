package com.shopwizard.catalog.mapper;

import com.shopwizard.catalog.model.Brand;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface BrandMapper {
    List<Brand> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Brand select(Map<String, Object> params);
    int insert(Brand brand);
    int update(Brand brand);
    int delete(Map<String, Object> params);
}
