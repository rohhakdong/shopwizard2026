package com.shopwizard.ship.mapper;

import com.shopwizard.ship.model.ShipCmplet;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ShipCmpletMapper {
    List<ShipCmplet> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ShipCmplet select(Map<String, Object> params);
    int insert(ShipCmplet shipCmplet);
    int update(ShipCmplet shipCmplet);
    int delete(ShipCmplet shipCmplet);
}
