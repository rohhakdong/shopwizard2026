package com.shopwizard.company.mapper;

import com.shopwizard.company.model.DeliComp;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface DeliCompMapper {
    List<DeliComp> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    DeliComp select(Map<String, Object> params);
    int insert(DeliComp deliComp);
    int update(DeliComp deliComp);
    int delete(DeliComp deliComp);
}
