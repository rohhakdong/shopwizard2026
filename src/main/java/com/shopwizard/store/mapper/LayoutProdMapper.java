package com.shopwizard.store.mapper;

import com.shopwizard.store.model.LayoutProd;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface LayoutProdMapper {
    List<LayoutProd> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    int insert(LayoutProd layoutProd);
    int update(LayoutProd layoutProd);
    int delete(Map<String, Object> params);
}
