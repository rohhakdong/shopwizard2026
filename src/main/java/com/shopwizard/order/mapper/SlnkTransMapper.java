package com.shopwizard.order.mapper;

import com.shopwizard.order.model.SlnkTrans;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SlnkTransMapper {
    List<SlnkTrans> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    SlnkTrans select(Map<String, Object> params);
    /** 특정 결과타입(예: '진행')인 이력 건수. 동시 수집 방지용. */
    int selectCountByResultType(String resultType);
    void insert(SlnkTrans slnkTrans);
    void update(SlnkTrans slnkTrans);
}
