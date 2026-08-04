package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustCpnDetailMapper;
import com.shopwizard.profile.model.CustCpnDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustCpnDetailService {

    private final CustCpnDetailMapper custCpnDetailMapper;

    public List<CustCpnDetail> selectList(Map<String, Object> params) { return custCpnDetailMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custCpnDetailMapper.selectCount(params); }
    public CustCpnDetail select(Map<String, Object> params) { return custCpnDetailMapper.select(params); }
    public int insert(CustCpnDetail custCpnDetail) { return custCpnDetailMapper.insert(custCpnDetail); }
    public int update(CustCpnDetail custCpnDetail) { return custCpnDetailMapper.update(custCpnDetail); }
    public int disable(Map<String, Object> params) { return custCpnDetailMapper.disable(params); }
    public int enable(Map<String, Object> params) { return custCpnDetailMapper.enable(params); }
    public int delete(Map<String, Object> params) { return custCpnDetailMapper.delete(params); }
}
