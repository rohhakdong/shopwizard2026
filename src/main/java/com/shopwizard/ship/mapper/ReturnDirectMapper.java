package com.shopwizard.ship.mapper;

import com.shopwizard.ship.model.ReturnDirect;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReturnDirectMapper {
    List<ReturnDirect> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ReturnDirect select(Map<String, Object> params);
    int insert(ReturnDirect returnDirect);
    int insertReturnDirect(Map<String, Object> params);
    int update(Map<String, Object> params);
    int updateAdjustSelectDate(Map<String, Object> params);
    int updateAdjustSelectDateSchedule(Map<String, Object> params);
    int delete(ReturnDirect returnDirect);
    int deleteRefundCancel(Map<String, Object> params);
}
