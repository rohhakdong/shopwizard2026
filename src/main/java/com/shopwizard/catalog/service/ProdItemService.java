package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdItemMapper;
import com.shopwizard.catalog.model.ProdItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service("catalogProdItemService")
@RequiredArgsConstructor
public class ProdItemService {
    private final ProdItemMapper prodItemMapper;

    public List<ProdItem> selectList(Map<String, Object> params) { return prodItemMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodItemMapper.selectCount(params); }
    public ProdItem select(Map<String, Object> params) { return prodItemMapper.select(params); }
    public String selectItemCodeByAttrVal(Map<String, Object> params) { return prodItemMapper.selectItemCodeByAttrVal(params); }
    @Transactional public int insert(ProdItem prodItem) { return prodItemMapper.insert(prodItem); }
    @Transactional public int update(ProdItem prodItem) { return prodItemMapper.update(prodItem); }
    @Transactional public int updateItemlist(Map<String, Object> params) { return prodItemMapper.updateItemlist(params); }
    @Transactional public int delete(ProdItem prodItem) { return prodItemMapper.delete(prodItem); }
    @Transactional public int disable(String prodCode) { return prodItemMapper.disable(prodCode); }
    @Transactional public int enable(Map<String, Object> params) { return prodItemMapper.enable(params); }
    @Transactional public int copy2Shopion(Map<String, Object> params) { return prodItemMapper.copy2Shopion(params); }
    @Transactional public int delete2Shopion(String prodCode) { return prodItemMapper.delete2Shopion(prodCode); }
}
