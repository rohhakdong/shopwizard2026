package com.shopwizard.operate.mapper;

import com.shopwizard.operate.model.EntrstProd;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface EntrstProdMapper {
    List<EntrstProd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    EntrstProd select(Map<String, Object> params);
    void insert(EntrstProd entrstProd);
    void update(EntrstProd entrstProd);
    void updateByDeli(Map<String, Object> params);
    void updateByPrice(Map<String, Object> params);
    void updateByBank(Map<String, Object> params);
    void updateByLoan(Map<String, Object> params);
    void delete(EntrstProd entrstProd);
}
