package com.shopwizard.catalog.mapper;

import com.shopwizard.catalog.model.ProdItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
@org.springframework.stereotype.Component("catalogProdItemMapper")
public interface ProdItemMapper {
    List<ProdItem> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ProdItem select(Map<String, Object> params);
    String selectItemCodeByAttrVal(Map<String, Object> params);
    int insert(ProdItem prodItem);
    int update(ProdItem prodItem);
    int updateItemlist(Map<String, Object> params);
    int delete(ProdItem prodItem);
    int deleteByProdCode(String prodCode);
    int disable(String prodCode);
    int enable(Map<String, Object> params);
    int copy2Shopion(Map<String, Object> params);
    int delete2Shopion(String prodCode);
}
