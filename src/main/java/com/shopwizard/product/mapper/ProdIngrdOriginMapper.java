package com.shopwizard.product.mapper;

import com.shopwizard.product.model.ProdIngrdOrigin;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProdIngrdOriginMapper {
    List<ProdIngrdOrigin> selectList(Map<String, Object> params);
}
