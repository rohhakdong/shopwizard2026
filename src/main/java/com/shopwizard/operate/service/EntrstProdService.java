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
    public void insert(EntrstProd entrstProd) { entrstProdMapper.insert(entrstProd); }
    public void update(EntrstProd entrstProd) { entrstProdMapper.update(entrstProd); }
    public void updateByDeli(Map<String, Object> params) { entrstProdMapper.updateByDeli(params); }
    public void updateByPrice(Map<String, Object> params) { entrstProdMapper.updateByPrice(params); }
    public void updateByBank(Map<String, Object> params) { entrstProdMapper.updateByBank(params); }
    public void updateByLoan(Map<String, Object> params) { entrstProdMapper.updateByLoan(params); }
    public void delete(EntrstProd entrstProd) { entrstProdMapper.delete(entrstProd); }
}
