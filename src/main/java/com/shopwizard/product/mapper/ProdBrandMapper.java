package com.shopwizard.product.mapper;

import com.shopwizard.product.model.ProdBrand;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProdBrandMapper {
    List<ProdBrand> selectList(Map<String, Object> params);
    ProdBrand select(Map<String, Object> params);
    void insert(ProdBrand prodBrand);
    void update(ProdBrand prodBrand);
    void delete(Map<String, Object> params);
}
