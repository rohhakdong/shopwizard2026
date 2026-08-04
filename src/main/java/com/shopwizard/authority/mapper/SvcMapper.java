package com.shopwizard.authority.mapper;

import com.shopwizard.authority.model.Svc;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface SvcMapper {
    List<Svc> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    Svc select(Map<String, Object> params);
    int insert(Svc svc);
    int update(Svc svc);
    int delete(Svc svc);
}
