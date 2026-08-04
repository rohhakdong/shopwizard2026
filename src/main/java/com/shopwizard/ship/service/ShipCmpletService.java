package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.ShipCmpletMapper;
import com.shopwizard.ship.model.ShipCmplet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipCmpletService {
    private final ShipCmpletMapper shipCmpletMapper;
    public List<ShipCmplet> selectList(Map<String, Object> params) { return shipCmpletMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return shipCmpletMapper.selectCount(params); }
    public ShipCmplet select(Map<String, Object> params) { return shipCmpletMapper.select(params); }
    public int insert(ShipCmplet shipCmplet) { return shipCmpletMapper.insert(shipCmplet); }
    public int update(ShipCmplet shipCmplet) { return shipCmpletMapper.update(shipCmplet); }
    public int delete(ShipCmplet shipCmplet) { return shipCmpletMapper.delete(shipCmplet); }
}
