package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.DeptApprovLine;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeptApprovLineMapper {
    List<DeptApprovLine> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    DeptApprovLine select(Map<String, Object> params);
    int insert(DeptApprovLine deptApprovLine);
    int update(DeptApprovLine deptApprovLine);
    int delete(Map<String, Object> params);
}
