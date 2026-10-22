package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustConcrnProdMapper;
import com.shopwizard.profile.model.CustConcrnProd;
import com.shopwizard.profile.model.CustConcrnProdReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustConcrnProdService {

    private final CustConcrnProdMapper custConcrnProdMapper;

    public List<CustConcrnProd> selectList(Map<String, Object> params) { return custConcrnProdMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custConcrnProdMapper.selectCount(params); }
    public List<CustConcrnProdReport> selectListReport(Map<String, Object> params) { return custConcrnProdMapper.selectListReport(params); }
    public int selectCountReport(Map<String, Object> params) { return custConcrnProdMapper.selectCountReport(params); }
    public CustConcrnProd select(Map<String, Object> params) { return custConcrnProdMapper.select(params); }
}
