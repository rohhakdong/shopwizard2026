package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.Estimt;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface EstimtMapper {
    List<Estimt> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Estimt select(Map<String, Object> params);
    List<Estimt> selectListStatDept(Map<String, Object> params);
    Double selectMonthlyTotalAmtDeptAccnt(Map<String, Object> params);
    int insert(Estimt estimt);
    int update(Estimt estimt);
    int delete(Map<String, Object> params);
}
