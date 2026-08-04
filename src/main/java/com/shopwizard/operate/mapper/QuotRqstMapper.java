package com.shopwizard.operate.mapper;

import com.shopwizard.operate.model.QuotRqst;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface QuotRqstMapper {
    List<QuotRqst> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    QuotRqst select(Map<String, Object> params);
    void insert(QuotRqst quotRqst);
    void update(QuotRqst quotRqst);
    void delete(QuotRqst quotRqst);
}
