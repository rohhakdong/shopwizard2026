package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.ShipCheckMapper;
import com.shopwizard.ship.model.ShipCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipCheckService {
    private final ShipCheckMapper shipCheckMapper;
    public List<ShipCheck> selectList(Map<String, Object> params) { return shipCheckMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return shipCheckMapper.selectCount(params); }
    public ShipCheck select(Map<String, Object> params) { return shipCheckMapper.select(params); }
    public int insert(ShipCheck shipCheck) { return shipCheckMapper.insert(shipCheck); }
    public int updatePrintCount(ShipCheck shipCheck) { return shipCheckMapper.updatePrintCount(shipCheck); }
    public int delete(ShipCheck shipCheck) { return shipCheckMapper.delete(shipCheck); }
}
