package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustPointDetailMapper;
import com.shopwizard.profile.model.CustPointDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustPointDetailService {

    private final CustPointDetailMapper custPointDetailMapper;

    public List<CustPointDetail> selectList(Map<String, Object> params) { return custPointDetailMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custPointDetailMapper.selectCount(params); }
    public CustPointDetail select(Map<String, Object> params) { return custPointDetailMapper.select(params); }
    public Integer selectSum(Map<String, Object> params) { return custPointDetailMapper.selectSum(params); }
    public int insert(CustPointDetail custPointDetail) { return custPointDetailMapper.insert(custPointDetail); }
    public int insertSchedule(Map<String, Object> params) { return custPointDetailMapper.insertSchedule(params); }
    public int insertRefund(Map<String, Object> params) { return custPointDetailMapper.insertRefund(params); }
    public int update(CustPointDetail custPointDetail) { return custPointDetailMapper.update(custPointDetail); }
    public int delete(Map<String, Object> params) { return custPointDetailMapper.delete(params); }
}
