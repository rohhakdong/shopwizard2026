package com.shopwizard.product.service;

import com.shopwizard.product.mapper.ProdMapper;
import com.shopwizard.product.model.Prod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdService {
    private final ProdMapper prodMapper;

    public List<Prod> selectList(Map<String, Object> params) { return prodMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodMapper.selectCount(params); }
    public Prod select(Map<String, Object> params) { return prodMapper.select(params); }
    public Prod selectByShopProdCode(Map<String, Object> params) { return prodMapper.selectByShopProdCode(params); }
    public String selectShopProdCode(String shopProdCode) { return prodMapper.selectShopProdCode(shopProdCode); }
    public void insert(Prod prod) { prodMapper.insert(prod); }
    public void update(Prod prod) { prodMapper.update(prod); }
    public void updateReviewCntnts(Prod prod) { prodMapper.updateReviewCntnts(prod); }
    public void delete(String prodCode) { prodMapper.delete(prodCode); }
    public List<Prod> selectListSearch(Map<String, Object> params) { return prodMapper.selectListSearch(params); }
    public int selectCountSearch(Map<String, Object> params) { return prodMapper.selectCountSearch(params); }
    public List<Prod> selectListKeyword(Map<String, Object> params) { return prodMapper.selectListKeyword(params); }
    public List<Prod> selectListBrand(Map<String, Object> params) { return prodMapper.selectListBrand(params); }
    public int selectSupplyQty(Map<String, Object> params) { return prodMapper.selectSupplyQty(params); }
    public int selectSupplyQtyItem(Map<String, Object> params) { return prodMapper.selectSupplyQtyItem(params); }
    public void updateSupplyQty(Map<String, Object> params) { prodMapper.updateSupplyQty(params); }
    public void updateSupplyQtyItem(Map<String, Object> params) { prodMapper.updateSupplyQtyItem(params); }
    public void updateCatSupplyQty(Map<String, Object> params) { prodMapper.updateCatSupplyQty(params); }
    public void updateCatSupplyQtyItem(Map<String, Object> params) { prodMapper.updateCatSupplyQtyItem(params); }
    public void updateSaleYn(Map<String, Object> params) { prodMapper.updateSaleYn(params); }
    public void updateSaleYnItem(Map<String, Object> params) { prodMapper.updateSaleYnItem(params); }
    public void updateCatSaleYn(Map<String, Object> params) { prodMapper.updateCatSaleYn(params); }
    public void updateCatSaleYnItem(Map<String, Object> params) { prodMapper.updateCatSaleYnItem(params); }
    public List<Map<String, Object>> selectListNaver(Map<String, Object> params) { return prodMapper.selectListNaver(params); }
}
