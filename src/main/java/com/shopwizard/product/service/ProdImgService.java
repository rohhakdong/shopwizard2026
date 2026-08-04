package com.shopwizard.product.service;

import com.shopwizard.product.mapper.ProdImgMapper;
import com.shopwizard.product.model.ProdImg;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdImgService {
    private final ProdImgMapper prodImgMapper;

    public List<ProdImg> selectList(Map<String, Object> params) { return prodImgMapper.selectList(params); }
}
