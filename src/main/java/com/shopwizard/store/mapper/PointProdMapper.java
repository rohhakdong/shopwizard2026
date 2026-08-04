package com.shopwizard.store.mapper;

import com.shopwizard.store.model.PointProd;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface PointProdMapper {
    List<PointProd> selectList(Map<String, Object> params);
    int insert(PointProd pointProd);
    int delete(Map<String, Object> params);
}
