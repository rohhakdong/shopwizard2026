package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.Accnt;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface AccntMapper {
    List<Accnt> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Accnt select(Integer accntId);
    int insert(Accnt accnt);
    int update(Accnt accnt);
    int delete(Integer accntId);
}
