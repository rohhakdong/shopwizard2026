package com.shopwizard.product.mapper;

import com.shopwizard.product.model.SlinkLog;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface SlinkLogMapper {
    List<SlinkLog> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    SlinkLog select(Map<String, Object> params);
    int insert(SlinkLog slinkLog);
    int update(SlinkLog slinkLog);
    int delete(SlinkLog slinkLog);
}
