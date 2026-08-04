package com.shopwizard.store.mapper;

import com.shopwizard.store.model.Design;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface DesignMapper {
    List<Design> selectList(Map<String, Object> params);
    int insert(Design design);
    int update(Design design);
    int delete(Integer designId);
}
