package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.Branch;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface BranchMapper {
    List<Branch> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Branch select(Integer branchId);
    int insert(Branch branch);
    int update(Branch branch);
    int delete(Integer branchId);
}
