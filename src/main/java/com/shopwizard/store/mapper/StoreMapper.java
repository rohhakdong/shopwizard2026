package com.shopwizard.store.mapper;

import com.shopwizard.store.model.Store;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface StoreMapper {
    List<Store> selectList(Map<String, Object> params);
    String selectStoreCodeMax(Map<String, Object> params);
    List<Store> selectListTable(Map<String, Object> params);
    Store select(Map<String, Object> params);
    List<Store> selectListLevel(Map<String, Object> params);
    int insert(Store store);
    int update(Store store);
    int delete(Map<String, Object> params);
}
