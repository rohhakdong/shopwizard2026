package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustApprovLineMapper;
import com.shopwizard.profile.model.CustApprovLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustApprovLineService {

    private final CustApprovLineMapper custApprovLineMapper;

    public List<CustApprovLine> selectList(Map<String, Object> params) { return custApprovLineMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return custApprovLineMapper.selectCount(params); }
    public CustApprovLine select(Map<String, Object> params) { return custApprovLineMapper.select(params); }
    public Integer selectApprovLevelMax(Integer custId) { return custApprovLineMapper.selectApprovLevelMax(custId); }
    public int insert(CustApprovLine custApprovLine) { return custApprovLineMapper.insert(custApprovLine); }
    public int update(CustApprovLine custApprovLine) { return custApprovLineMapper.update(custApprovLine); }
    public int delete(Map<String, Object> params) { return custApprovLineMapper.delete(params); }
}
