package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.EstimtMapper;
import com.shopwizard.profile.model.Estimt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class EstimtService {

    private final EstimtMapper estimtMapper;

    public List<Estimt> selectList(Map<String, Object> params) { return estimtMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return estimtMapper.selectCount(params); }
    public Estimt select(Map<String, Object> params) { return estimtMapper.select(params); }
    public List<Estimt> selectListStatDept(Map<String, Object> params) { return estimtMapper.selectListStatDept(params); }
    public Double selectMonthlyTotalAmtDeptAccnt(Map<String, Object> params) { return estimtMapper.selectMonthlyTotalAmtDeptAccnt(params); }
    public int insert(Estimt estimt) { return estimtMapper.insert(estimt); }
    public int update(Estimt estimt) { return estimtMapper.update(estimt); }
    public int delete(Map<String, Object> params) { return estimtMapper.delete(params); }
}
