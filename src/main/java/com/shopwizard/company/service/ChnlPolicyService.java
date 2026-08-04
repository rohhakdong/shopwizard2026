package com.shopwizard.company.service;

import com.shopwizard.company.mapper.ChnlPolicyMapper;
import com.shopwizard.company.model.ChnlPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChnlPolicyService {
    private final ChnlPolicyMapper chnlPolicyMapper;

    public List<ChnlPolicy> getList(Map<String, Object> params) { return chnlPolicyMapper.selectList(params); }
    public int getCount(Map<String, Object> params) { return chnlPolicyMapper.selectCount(params); }
    public ChnlPolicy get(Map<String, Object> params) { return chnlPolicyMapper.select(params); }
    public String getMax() { return chnlPolicyMapper.selectMax(); }

    @Transactional
    public int insert(ChnlPolicy chnlPolicy) { return chnlPolicyMapper.insert(chnlPolicy); }
    @Transactional
    public int update(ChnlPolicy chnlPolicy) { return chnlPolicyMapper.update(chnlPolicy); }
    @Transactional
    public int delete(ChnlPolicy chnlPolicy) { return chnlPolicyMapper.delete(chnlPolicy); }
}
