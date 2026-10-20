package com.shopwizard.order.mapper;

import com.shopwizard.order.model.Basket;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface BasketMapper {
    List<Basket> selectList(Map<String, Object> params);
    Basket select(Map<String, Object> params);
    Basket selectByItem(Map<String, Object> params);
    int countProd(String prodCode);
    void insert(Basket basket);
    int update(Map<String, Object> params);
    int delete(Map<String, Object> params);
    int deleteMulti(Map<String, Object> params);
    int clean(Map<String, Object> params);
}
