package com.shopwizard.operate.service;

import com.shopwizard.operate.mapper.FaqMapper;
import com.shopwizard.operate.model.Faq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FaqService {
    private final FaqMapper faqMapper;

    public List<Faq> selectList(Map<String, Object> params) { return faqMapper.selectList(params); }
    public Faq select(Integer faqNo) { return faqMapper.select(faqNo); }
    public String selectFaqCntnts(Integer faqNo) { return faqMapper.selectFaqCntnts(faqNo); }
}
