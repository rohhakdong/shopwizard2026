package com.shopwizard.catalog.mapper;

import com.shopwizard.catalog.model.ProdImg;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
@org.springframework.stereotype.Component("catalogProdImgMapper")
public interface ProdImgMapper {
    List<ProdImg> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ProdImg select(Map<String, Object> params);
    String selectMaxCode(String prodCode);
    int insert(ProdImg prodImg);
    int update(ProdImg prodImg);
    int delete(ProdImg prodImg);
    int copy2Shopion(Map<String, Object> params);
    int delete2Shopion(String prodCode);
}
