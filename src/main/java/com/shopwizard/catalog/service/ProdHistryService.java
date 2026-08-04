package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdHistryMapper;
import com.shopwizard.catalog.model.ProdHistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProdHistryService {
    private final ProdHistryMapper prodHistryMapper;

    public List<ProdHistry> selectList(Map<String, Object> params) { return prodHistryMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodHistryMapper.selectCount(params); }
    public ProdHistry select(Map<String, Object> params) { return prodHistryMapper.select(params); }
    @Transactional public int insert(ProdHistry prodHistry) { return prodHistryMapper.insert(prodHistry); }
    @Transactional public int update(ProdHistry prodHistry) { return prodHistryMapper.update(prodHistry); }
    @Transactional public int delete(ProdHistry prodHistry) { return prodHistryMapper.delete(prodHistry); }
}
