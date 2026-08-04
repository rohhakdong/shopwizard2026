package com.shopwizard.company.mapper;

import com.shopwizard.company.model.ShopWarehs;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ShopWarehsMapper {
    List<ShopWarehs> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ShopWarehs select(Map<String, Object> params);
    int insert(ShopWarehs shopWarehs);
    int update(ShopWarehs shopWarehs);
    int delete(ShopWarehs shopWarehs);
}
