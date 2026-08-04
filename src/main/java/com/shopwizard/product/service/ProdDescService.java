package com.shopwizard.product.service;

import com.shopwizard.product.mapper.ProdDescMapper;
import com.shopwizard.product.model.ProdDesc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdDescService {
    private final ProdDescMapper prodDescMapper;

    public List<ProdDesc> selectList(Map<String, Object> params) { return prodDescMapper.selectList(params); }
}
