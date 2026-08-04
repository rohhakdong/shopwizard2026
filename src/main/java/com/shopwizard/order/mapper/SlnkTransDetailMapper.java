package com.shopwizard.order.mapper;

import com.shopwizard.order.model.SlnkTransDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SlnkTransDetailMapper {
    List<SlnkTransDetail> selectList(Map<String, Object> params);
    SlnkTransDetail select(Map<String, Object> params);
    void insert(SlnkTransDetail slnkTransDetail);
    void update(SlnkTransDetail slnkTransDetail);
    void delete(Map<String, Object> params);
}
