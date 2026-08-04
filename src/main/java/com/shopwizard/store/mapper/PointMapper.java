package com.shopwizard.store.mapper;

import com.shopwizard.store.model.Point;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface PointMapper {
    List<Point> selectList(Map<String, Object> params);
    Point select(Integer pointId);
    int insert(Point point);
    int update(Point point);
    int delete(Integer pointId);
}
