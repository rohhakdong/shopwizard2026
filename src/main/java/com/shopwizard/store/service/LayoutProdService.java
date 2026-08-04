package com.shopwizard.store.service;

import com.shopwizard.store.mapper.LayoutProdMapper;
import com.shopwizard.store.model.LayoutProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class LayoutProdService {
    private final LayoutProdMapper layoutProdMapper;

    public List<LayoutProd> selectList(Map<String, Object> params) { return layoutProdMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return layoutProdMapper.selectCount(params); }
    public int insert(LayoutProd layoutProd) { return layoutProdMapper.insert(layoutProd); }
    public int update(LayoutProd layoutProd) { return layoutProdMapper.update(layoutProd); }
    public int delete(Map<String, Object> params) { return layoutProdMapper.delete(params); }
}
