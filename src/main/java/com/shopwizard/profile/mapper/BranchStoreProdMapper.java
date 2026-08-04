package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.BranchStoreProd;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface BranchStoreProdMapper {
    List<BranchStoreProd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    BranchStoreProd select(Map<String, Object> params);
    int insert(BranchStoreProd branchStoreProd);
    int update(BranchStoreProd branchStoreProd);
    int delete(Map<String, Object> params);
}
