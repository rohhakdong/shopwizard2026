package com.shopwizard.order.mapper;

import com.shopwizard.order.model.Basket;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface BasketMapper {
    List<Basket> selectListFull(Map<String, Object> params);
    Basket selectDirect(Map<String, Object> params);
    List<Basket> selectList(Map<String, Object> params);
    Basket select(Map<String, Object> params);
    Basket selectByItem(Map<String, Object> params);
    void insert(Basket basket);
    void update(Map<String, Object> params);
    void updateCustId(Map<String, Object> params);
    void updateMro(Map<String, Object> params);
    void delete(Map<String, Object> params);
    void deleteMulti(Map<String, Object> params);
    void clean(Map<String, Object> params);
}
