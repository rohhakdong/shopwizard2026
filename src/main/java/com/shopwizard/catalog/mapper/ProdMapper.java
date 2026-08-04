package com.shopwizard.catalog.mapper;

import com.shopwizard.catalog.model.Prod;
import com.shopwizard.catalog.model.ProdDashBoard;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
@org.springframework.stereotype.Component("catalogProdMapper")
public interface ProdMapper {
    List<Prod> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Prod select(String prodCode);
    String selectProdDesc(String prodCode);
    String selectProdCode(String shopProdCode);
    List<ProdDashBoard> selectListDashboard(Map<String, Object> params);
    int insert(Prod prod);
    int update(Prod prod);
    int updatePrice(Map<String, Object> params);
    int approv(Map<String, Object> params);
    int delete(Prod prod);
    int copy2Shopion(Map<String, Object> params);
    int delete2Shopion(String prodCode);
    int delete2ShopionStoreProd(String prodCode);
    int delete2ShopionLayoutProd(String prodCode);
    int delete2ShopionEventProd(String prodCode);
}
