package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.BrandMapper;
import com.shopwizard.catalog.model.Brand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandMapper brandMapper;

    public List<Brand> selectList(Map<String, Object> params) { return brandMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return brandMapper.selectCount(params); }
    public Brand select(Map<String, Object> params) { return brandMapper.select(params); }
    @Transactional public int insert(Brand brand) { return brandMapper.insert(brand); }
    @Transactional public int update(Brand brand) { return brandMapper.update(brand); }
    @Transactional public int delete(Map<String, Object> params) { return brandMapper.delete(params); }
}
