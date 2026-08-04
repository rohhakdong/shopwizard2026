package com.shopwizard.product.mapper;

import com.shopwizard.product.model.ProdImg;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProdImgMapper {
    List<ProdImg> selectList(Map<String, Object> params);
}
