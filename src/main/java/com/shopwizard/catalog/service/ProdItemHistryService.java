package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdItemHistryMapper;
import com.shopwizard.catalog.model.ProdItemHistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProdItemHistryService {
    private final ProdItemHistryMapper prodItemHistryMapper;

    public List<ProdItemHistry> selectList(Map<String, Object> params) { return prodItemHistryMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return prodItemHistryMapper.selectCount(params); }
    public ProdItemHistry select(Map<String, Object> params) { return prodItemHistryMapper.select(params); }
    @Transactional public int insert(ProdItemHistry prodItemHistry) { return prodItemHistryMapper.insert(prodItemHistry); }
    @Transactional public int update(ProdItemHistry prodItemHistry) { return prodItemHistryMapper.update(prodItemHistry); }
    @Transactional public int delete(ProdItemHistry prodItemHistry) { return prodItemHistryMapper.delete(prodItemHistry); }
}
