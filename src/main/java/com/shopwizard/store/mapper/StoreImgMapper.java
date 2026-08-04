package com.shopwizard.store.mapper;

import com.shopwizard.store.model.StoreImg;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface StoreImgMapper {
    List<StoreImg> selectList(Map<String, Object> params);
    StoreImg select(Map<String, Object> params);
    String selectStoreImgHtml(Map<String, Object> params);
    int insert(StoreImg storeImg);
    int update(StoreImg storeImg);
    int delete(Map<String, Object> params);
}
