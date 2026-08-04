package com.shopwizard.product.mapper;

import com.shopwizard.product.model.Prod;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProdMapper {
    List<Prod> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Prod select(Map<String, Object> params);
    Prod selectByShopProdCode(Map<String, Object> params);
    String selectShopProdCode(String shopProdCode);
    void insert(Prod prod);
    void update(Prod prod);
    void updateReviewCntnts(Prod prod);
    void delete(String prodCode);
    List<Prod> selectListSearch(Map<String, Object> params);
    int selectCountSearch(Map<String, Object> params);
    List<Prod> selectListKeyword(Map<String, Object> params);
    List<Prod> selectListBrand(Map<String, Object> params);
    int selectSupplyQty(Map<String, Object> params);
    int selectSupplyQtyItem(Map<String, Object> params);
    void updateSupplyQty(Map<String, Object> params);
    void updateSupplyQtyItem(Map<String, Object> params);
    void updateCatSupplyQty(Map<String, Object> params);
    void updateCatSupplyQtyItem(Map<String, Object> params);
    void updateSaleYn(Map<String, Object> params);
    void updateSaleYnItem(Map<String, Object> params);
    void updateCatSaleYn(Map<String, Object> params);
    void updateCatSaleYnItem(Map<String, Object> params);
    List<Map<String, Object>> selectListNaver(Map<String, Object> params);
}
