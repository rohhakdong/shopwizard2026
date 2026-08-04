package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderProdApprovLine;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderProdApprovLineMapper {
    List<OrderProdApprovLine> selectList(Map<String, Object> params);
    int count(Map<String, Object> params);
    int countReady(Map<String, Object> params);
    int countApprov(Map<String, Object> params);
    int countReject(Map<String, Object> params);
    void insert(Map<String, Object> params);
    void updateApprov(Map<String, Object> params);
    void updateReject(Map<String, Object> params);
    void delete(Map<String, Object> params);
}
