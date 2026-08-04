package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.CustPointDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustPointDetailMapper {
    List<CustPointDetail> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    CustPointDetail select(Map<String, Object> params);
    Integer selectSum(Map<String, Object> params);
    int insert(CustPointDetail custPointDetail);
    int insertSchedule(Map<String, Object> params);
    int insertRefund(Map<String, Object> params);
    int update(CustPointDetail custPointDetail);
    int delete(Map<String, Object> params);
}
