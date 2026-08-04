package com.shopwizard.adjust.service;

import com.shopwizard.adjust.mapper.AdjustCheckRqstMapper;
import com.shopwizard.adjust.model.AdjustCheckRqst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AdjustCheckRqstService {

    private final AdjustCheckRqstMapper adjustCheckRqstMapper;

    public List<AdjustCheckRqst> selectList(Map<String, Object> params) { return adjustCheckRqstMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return adjustCheckRqstMapper.selectCount(params); }
    public AdjustCheckRqst select(Map<String, Object> params) { return adjustCheckRqstMapper.select(params); }
    public void insert(AdjustCheckRqst adjustCheckRqst) { adjustCheckRqstMapper.insert(adjustCheckRqst); }
    public void update(AdjustCheckRqst adjustCheckRqst) { adjustCheckRqstMapper.update(adjustCheckRqst); }
    public void delete(AdjustCheckRqst adjustCheckRqst) { adjustCheckRqstMapper.delete(adjustCheckRqst); }
}
