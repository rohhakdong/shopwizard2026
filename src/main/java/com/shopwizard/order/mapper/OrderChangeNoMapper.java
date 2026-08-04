package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderChangeNo;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface OrderChangeNoMapper {
    int selectCount(Map<String, Object> params);
    int selectMax(Map<String, Object> params);
    Integer selectChangeNo(Map<String, Object> params);
    void insert(Map<String, Object> params);
}
