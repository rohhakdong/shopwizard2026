package com.shopwizard.profile.mapper;

import com.shopwizard.profile.model.NiceNameCheck;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface NiceNameCheckMapper {
    List<NiceNameCheck> selectList(Map<String, Object> params);
    NiceNameCheck select(String nameCheckUid);
    int insert(NiceNameCheck niceNameCheck);
}
