package com.shopwizard.product.mapper;

import com.shopwizard.product.model.ProdDeliFee;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProdDeliFeeMapper {
    List<ProdDeliFee> selectList(Map<String, Object> params);
    ProdDeliFee select(Integer deliFeeId);
    void insert(ProdDeliFee prodDeliFee);
    void update(ProdDeliFee prodDeliFee);
    void delete(Integer deliFeeId);
}
