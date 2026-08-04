package com.shopwizard.product.service;

import com.shopwizard.product.mapper.ProdDeliFeeMapper;
import com.shopwizard.product.model.ProdDeliFee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdDeliFeeService {
    private final ProdDeliFeeMapper prodDeliFeeMapper;

    public List<ProdDeliFee> selectList(Map<String, Object> params) { return prodDeliFeeMapper.selectList(params); }
    public ProdDeliFee select(Integer deliFeeId) { return prodDeliFeeMapper.select(deliFeeId); }
    public void insert(ProdDeliFee prodDeliFee) { prodDeliFeeMapper.insert(prodDeliFee); }
    public void update(ProdDeliFee prodDeliFee) { prodDeliFeeMapper.update(prodDeliFee); }
    public void delete(Integer deliFeeId) { prodDeliFeeMapper.delete(deliFeeId); }
}
