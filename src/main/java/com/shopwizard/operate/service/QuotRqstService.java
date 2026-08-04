package com.shopwizard.operate.service;

import com.shopwizard.operate.mapper.QuotRqstMapper;
import com.shopwizard.operate.model.QuotRqst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class QuotRqstService {
    private final QuotRqstMapper quotRqstMapper;

    public List<QuotRqst> selectList(Map<String, Object> params) { return quotRqstMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return quotRqstMapper.selectCount(params); }
    public QuotRqst select(Map<String, Object> params) { return quotRqstMapper.select(params); }
    public void insert(QuotRqst quotRqst) { quotRqstMapper.insert(quotRqst); }
    public void update(QuotRqst quotRqst) { quotRqstMapper.update(quotRqst); }
    public void delete(QuotRqst quotRqst) { quotRqstMapper.delete(quotRqst); }
}
