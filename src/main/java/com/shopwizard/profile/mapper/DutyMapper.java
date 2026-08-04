package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.Duty;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DutyMapper {
    List<Duty> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Duty select(Integer dutyId);
    int insert(Duty duty);
    int update(Duty duty);
    int delete(Integer dutyId);
}
