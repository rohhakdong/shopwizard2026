package com.shopwizard.ship.mapper;

import com.shopwizard.ship.model.ShipCheck;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ShipCheckMapper {
    List<ShipCheck> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ShipCheck select(Map<String, Object> params);
    int insert(ShipCheck shipCheck);
    int updatePrintCount(ShipCheck shipCheck);
    int delete(ShipCheck shipCheck);
}
