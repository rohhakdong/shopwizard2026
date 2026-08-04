package com.shopwizard.catalog.mapper;

import com.shopwizard.catalog.model.ProdChnl;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProdChnlMapper {
    List<ProdChnl> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ProdChnl select(Map<String, Object> params);
    int insert(ProdChnl prodChnl);
    int insertProdCodes(Map<String, Object> params);
    int update(ProdChnl prodChnl);
    int delete(ProdChnl prodChnl);
    int copy2Shopion(Map<String, Object> params);
    int delete2Shopion(String prodCode);
    int delete2ShopionProdChnl(ProdChnl prodChnl);
}
