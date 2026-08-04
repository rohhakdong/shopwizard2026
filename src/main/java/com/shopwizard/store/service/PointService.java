package com.shopwizard.store.service;

import com.shopwizard.store.mapper.PointMapper;
import com.shopwizard.store.model.Point;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PointService {
    private final PointMapper pointMapper;

    public List<Point> selectList(Map<String, Object> params) { return pointMapper.selectList(params); }
    public Point select(Integer pointId) { return pointMapper.select(pointId); }
    public int insert(Point point) { return pointMapper.insert(point); }
    public int update(Point point) { return pointMapper.update(point); }
    public int delete(Integer pointId) { return pointMapper.delete(pointId); }
}
