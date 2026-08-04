package com.shopwizard.company.mapper;

import com.shopwizard.company.model.Warehs;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface WarehsMapper {
    List<Warehs> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Warehs select(Map<String, Object> params);
    int insert(Warehs warehs);
    int update(Warehs warehs);
    int delete(Warehs warehs);
}
