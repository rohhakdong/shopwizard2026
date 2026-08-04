package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.BranchStore;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface BranchStoreMapper {
    List<BranchStore> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    BranchStore select(Map<String, Object> params);
    int insert(BranchStore branchStore);
    int update(BranchStore branchStore);
    int delete(Map<String, Object> params);
}
