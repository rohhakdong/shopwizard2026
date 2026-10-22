package com.shopwizard.code.mapper;

import com.shopwizard.code.model.ModelAttr;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ModelAttrMapper {
    List<ModelAttr> selectList(Map<String, Object> params);
}
