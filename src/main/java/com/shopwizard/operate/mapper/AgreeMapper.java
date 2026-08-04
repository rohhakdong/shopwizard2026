package com.shopwizard.operate.mapper;

import com.shopwizard.operate.model.Agree;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface AgreeMapper {
    List<Agree> selectList(Map<String, Object> params);
    Agree select(Map<String, Object> params);
    void insert(Agree agree);
    void update(Agree agree);
    void delete(Agree agree);
    String selectCntnts(Integer agreeNo);
}
