package com.shopwizard.product.mapper;

import com.shopwizard.product.model.ProdDesc;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProdDescMapper {
    List<ProdDesc> selectList(Map<String, Object> params);
}
