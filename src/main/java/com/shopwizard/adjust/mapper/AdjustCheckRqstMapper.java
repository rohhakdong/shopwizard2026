package com.shopwizard.adjust.mapper;

import com.shopwizard.adjust.model.AdjustCheckRqst;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdjustCheckRqstMapper {
    List<AdjustCheckRqst> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    AdjustCheckRqst select(Map<String, Object> params);
    int insert(AdjustCheckRqst adjustCheckRqst);
    int update(AdjustCheckRqst adjustCheckRqst);
    int delete(AdjustCheckRqst adjustCheckRqst);
}
