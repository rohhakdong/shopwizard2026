package com.shopwizard.code.mapper;

import com.shopwizard.code.model.ConstrVal;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ConstrValMapper {
    List<ConstrVal> findList(Map<String, Object> map);
    int insert(ConstrVal constrVal);
    int update(ConstrVal constrVal);
    int delete(ConstrVal constrVal);
}
