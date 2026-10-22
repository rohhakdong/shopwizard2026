package com.shopwizard.code.mapper;

import com.shopwizard.code.model.Model;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ModelMapper {
    List<Model> selectList(Map<String, Object> params);
    Model select(Integer modelId);
}
