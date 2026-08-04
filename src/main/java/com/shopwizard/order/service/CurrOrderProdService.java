package com.shopwizard.order.service;

import com.shopwizard.order.mapper.CurrOrderProdMapper;
import com.shopwizard.order.model.CurrOrderProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CurrOrderProdService {
    private final CurrOrderProdMapper currOrderProdMapper;
    public List<CurrOrderProd> selectList(Map<String, Object> params) { return currOrderProdMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return currOrderProdMapper.selectCount(params); }
    public CurrOrderProd select(Map<String, Object> params) { return currOrderProdMapper.select(params); }
    public int insert(CurrOrderProd currOrderProd) { return currOrderProdMapper.insert(currOrderProd); }
    public int insertMatching(Map<String, Object> params) { return currOrderProdMapper.insertMatching(params); }
    public int update(CurrOrderProd currOrderProd) { return currOrderProdMapper.update(currOrderProd); }
    public int delete(CurrOrderProd currOrderProd) { return currOrderProdMapper.delete(currOrderProd); }
}
