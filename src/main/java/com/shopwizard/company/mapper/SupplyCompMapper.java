package com.shopwizard.company.mapper;

import com.shopwizard.company.model.SupplyComp;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface SupplyCompMapper {
    List<SupplyComp> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    SupplyComp select(Map<String, Object> params);
    String selectMax(Map<String, Object> params);
    int insert(SupplyComp supplyComp);
    int update(SupplyComp supplyComp);
    int delete(Map<String, Object> params);
}
