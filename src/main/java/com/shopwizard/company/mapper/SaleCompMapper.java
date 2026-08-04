package com.shopwizard.company.mapper;

import com.shopwizard.company.model.SaleComp;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface SaleCompMapper {
    List<SaleComp> selectList(Map<String, Object> params);
    SaleComp select(Map<String, Object> params);
    String selectMax(Map<String, Object> params);
    int insert(SaleComp saleComp);
    int update(SaleComp saleComp);
    int delete(Map<String, Object> params);
}
