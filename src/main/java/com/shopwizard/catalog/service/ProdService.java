package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdMapper;
import com.shopwizard.catalog.model.Prod;
import com.shopwizard.catalog.model.ProdDashBoard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service("catalogProdService")
@RequiredArgsConstructor
public class ProdService {
    private final ProdMapper prodMapper;

    public List<Prod> selectList(Map<String, Object> params) { return prodMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodMapper.selectCount(params); }
    public Prod select(String prodCode) { return prodMapper.select(prodCode); }
    public String selectProdDesc(String prodCode) { return prodMapper.selectProdDesc(prodCode); }
    public String selectProdCode(String shopProdCode) { return prodMapper.selectProdCode(shopProdCode); }
    public List<ProdDashBoard> selectListDashboard(Map<String, Object> params) { return prodMapper.selectListDashboard(params); }
    @Transactional public int insert(Prod prod) { return prodMapper.insert(prod); }
    @Transactional public int update(Prod prod) { return prodMapper.update(prod); }
    @Transactional public int updatePrice(Map<String, Object> params) { return prodMapper.updatePrice(params); }
    @Transactional public int approv(Map<String, Object> params) { return prodMapper.approv(params); }
    @Transactional public int delete(Prod prod) { return prodMapper.delete(prod); }
    @Transactional public int copy2Shopion(Map<String, Object> params) { return prodMapper.copy2Shopion(params); }
    @Transactional public int delete2Shopion(String prodCode) { return prodMapper.delete2Shopion(prodCode); }
    @Transactional public int delete2ShopionStoreProd(String prodCode) { return prodMapper.delete2ShopionStoreProd(prodCode); }
    @Transactional public int delete2ShopionLayoutProd(String prodCode) { return prodMapper.delete2ShopionLayoutProd(prodCode); }
    @Transactional public int delete2ShopionEventProd(String prodCode) { return prodMapper.delete2ShopionEventProd(prodCode); }
}
