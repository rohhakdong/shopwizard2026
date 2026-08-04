package com.shopwizard.store.service;

import com.shopwizard.store.mapper.ProdCpnProdMapper;
import com.shopwizard.store.model.ProdCpnProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdCpnProdService {
    private final ProdCpnProdMapper prodCpnProdMapper;

    public List<ProdCpnProd> selectList(Map<String, Object> params) { return prodCpnProdMapper.selectList(params); }
    public int insert(ProdCpnProd prodCpnProd) { return prodCpnProdMapper.insert(prodCpnProd); }
    public int delete(Map<String, Object> params) { return prodCpnProdMapper.delete(params); }
}
