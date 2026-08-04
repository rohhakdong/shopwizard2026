package com.shopwizard.product.service;

import com.shopwizard.product.mapper.ProdItemMapper;
import com.shopwizard.product.model.ProdItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdItemService {
    private final ProdItemMapper prodItemMapper;

    public List<ProdItem> selectList(Map<String, Object> params) { return prodItemMapper.selectList(params); }
    public ProdItem select(Map<String, Object> params) { return prodItemMapper.select(params); }
}
