package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.RecentProd;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface RecentProdMapper {
    List<RecentProd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    RecentProd select(Map<String, Object> params);
    int insert(RecentProd recentProd);
    int update(RecentProd recentProd);
    int delete(Map<String, Object> params);
}
