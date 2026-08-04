package com.shopwizard.store.mapper;

import com.shopwizard.store.model.EventProd;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface EventProdMapper {
    List<EventProd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    List<EventProd> selectListAll(Map<String, Object> params);
    int insert(EventProd eventProd);
    int update(EventProd eventProd);
    int delete(Map<String, Object> params);
}
