package com.shopwizard.operate.service;

import com.shopwizard.operate.mapper.EntrstProdMapper;
import com.shopwizard.operate.model.EntrstProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class EntrstProdService {
    private final EntrstProdMapper entrstProdMapper;

    public List<EntrstProd> selectList(Map<String, Object> params) { return entrstProdMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return entrstProdMapper.selectCount(params); }
    public EntrstProd select(Map<String, Object> params) { return entrstProdMapper.select(params); }
}
