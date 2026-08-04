package com.shopwizard.store.service;

import com.shopwizard.store.mapper.PointProdMapper;
import com.shopwizard.store.model.PointProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PointProdService {
    private final PointProdMapper pointProdMapper;

    public List<PointProd> selectList(Map<String, Object> params) { return pointProdMapper.selectList(params); }
    public int insert(PointProd pointProd) { return pointProdMapper.insert(pointProd); }
    public int delete(Map<String, Object> params) { return pointProdMapper.delete(params); }
}
