package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdImgMapper;
import com.shopwizard.catalog.model.ProdImg;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service("catalogProdImgService")
@RequiredArgsConstructor
public class ProdImgService {
    private final ProdImgMapper prodImgMapper;

    public List<ProdImg> selectList(Map<String, Object> params) { return prodImgMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodImgMapper.selectCount(params); }
    public ProdImg select(Map<String, Object> params) { return prodImgMapper.select(params); }
    public String selectMaxCode(String prodCode) { return prodImgMapper.selectMaxCode(prodCode); }
    @Transactional public int insert(ProdImg prodImg) { return prodImgMapper.insert(prodImg); }
    @Transactional public int update(ProdImg prodImg) { return prodImgMapper.update(prodImg); }
    @Transactional public int delete(ProdImg prodImg) { return prodImgMapper.delete(prodImg); }
    @Transactional public int copy2Shopion(Map<String, Object> params) { return prodImgMapper.copy2Shopion(params); }
    @Transactional public int delete2Shopion(String prodCode) { return prodImgMapper.delete2Shopion(prodCode); }
}
