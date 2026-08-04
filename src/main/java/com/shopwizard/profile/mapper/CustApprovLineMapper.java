package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.CustApprovLine;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustApprovLineMapper {
    List<CustApprovLine> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    CustApprovLine select(Map<String, Object> params);
    Integer selectApprovLevelMax(Integer custId);
    int insert(CustApprovLine custApprovLine);
    int update(CustApprovLine custApprovLine);
    int delete(Map<String, Object> params);
}
