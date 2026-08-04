package com.shopwizard.store.mapper;

import com.shopwizard.store.model.StoreProd;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface StoreProdMapper {
    List<StoreProd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    List<StoreProd> selectListLocType(Map<String, Object> params);
    List<StoreProd> selectListOrderQty(Map<String, Object> params);
    String selectStoreCode(Map<String, Object> params);
    StoreProd select(Map<String, Object> params);
    int insert(StoreProd storeProd);
    int update(StoreProd storeProd);
    int delete(Map<String, Object> params);
    int deleteProd(Map<String, Object> params);
}
