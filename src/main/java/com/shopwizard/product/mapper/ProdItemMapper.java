package com.shopwizard.product.mapper;

import com.shopwizard.product.model.ProdItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProdItemMapper {
    List<ProdItem> selectList(Map<String, Object> params);
    ProdItem select(Map<String, Object> params);
}
