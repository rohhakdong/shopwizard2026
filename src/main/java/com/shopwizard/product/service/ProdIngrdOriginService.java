package com.shopwizard.product.service;

import com.shopwizard.product.mapper.ProdIngrdOriginMapper;
import com.shopwizard.product.model.ProdIngrdOrigin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdIngrdOriginService {
    private final ProdIngrdOriginMapper prodIngrdOriginMapper;

    public List<ProdIngrdOrigin> selectList(Map<String, Object> params) { return prodIngrdOriginMapper.selectList(params); }
}
