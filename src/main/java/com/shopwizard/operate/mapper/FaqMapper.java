package com.shopwizard.operate.mapper;

import com.shopwizard.operate.model.Faq;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface FaqMapper {
    List<Faq> selectList(Map<String, Object> params);
    Faq select(Integer faqNo);
    void insert(Faq faq);
    void update(Faq faq);
    void delete(Integer faqNo);
    String selectFaqCntnts(Integer faqNo);
}
