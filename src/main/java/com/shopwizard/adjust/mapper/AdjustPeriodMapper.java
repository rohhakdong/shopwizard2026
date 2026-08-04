package com.shopwizard.adjust.mapper;

import com.shopwizard.adjust.model.AdjustPeriod;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdjustPeriodMapper {
    List<AdjustPeriod> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    AdjustPeriod select(Map<String, Object> params);
    int insert(AdjustPeriod adjustPeriod);
    int update(AdjustPeriod adjustPeriod);
    int delete(AdjustPeriod adjustPeriod);
}
