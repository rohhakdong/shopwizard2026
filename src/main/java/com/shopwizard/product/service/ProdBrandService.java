package com.shopwizard.product.service;

import com.shopwizard.product.mapper.ProdBrandMapper;
import com.shopwizard.product.model.ProdBrand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdBrandService {
    private final ProdBrandMapper prodBrandMapper;

    public List<ProdBrand> selectList(Map<String, Object> params) { return prodBrandMapper.selectList(params); }
    public ProdBrand select(Map<String, Object> params) { return prodBrandMapper.select(params); }
    public void insert(ProdBrand prodBrand) { prodBrandMapper.insert(prodBrand); }
    public void update(ProdBrand prodBrand) { prodBrandMapper.update(prodBrand); }
    public void delete(Map<String, Object> params) { prodBrandMapper.delete(params); }
}
