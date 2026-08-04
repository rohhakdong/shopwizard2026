package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdChnlMapper;
import com.shopwizard.catalog.model.ProdChnl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProdChnlService {
    private final ProdChnlMapper prodChnlMapper;

    public List<ProdChnl> selectList(Map<String, Object> params) { return prodChnlMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodChnlMapper.selectCount(params); }
    public ProdChnl select(Map<String, Object> params) { return prodChnlMapper.select(params); }
    @Transactional public int insert(ProdChnl prodChnl) { return prodChnlMapper.insert(prodChnl); }
    @Transactional public int insertProdCodes(Map<String, Object> params) { return prodChnlMapper.insertProdCodes(params); }
    @Transactional public int update(ProdChnl prodChnl) { return prodChnlMapper.update(prodChnl); }
    @Transactional public int delete(ProdChnl prodChnl) { return prodChnlMapper.delete(prodChnl); }
    @Transactional public int copy2Shopion(Map<String, Object> params) { return prodChnlMapper.copy2Shopion(params); }
    @Transactional public int delete2Shopion(String prodCode) { return prodChnlMapper.delete2Shopion(prodCode); }
    @Transactional public int delete2ShopionProdChnl(ProdChnl prodChnl) { return prodChnlMapper.delete2ShopionProdChnl(prodChnl); }
}
