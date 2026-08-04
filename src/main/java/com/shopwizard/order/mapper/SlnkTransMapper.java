package com.shopwizard.order.mapper;

import com.shopwizard.order.model.SlnkTrans;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SlnkTransMapper {
    List<SlnkTrans> selectList(Map<String, Object> params);
    SlnkTrans select(Map<String, Object> params);
    void insert(SlnkTrans slnkTrans);
    void update(SlnkTrans slnkTrans);
    void delete(Map<String, Object> params);
}
