package com.shopwizard.order.service;

import com.shopwizard.order.mapper.BasketMapper;
import com.shopwizard.order.model.Basket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BasketService {
    private final BasketMapper basketMapper;

    public List<Basket> selectListFull(Map<String, Object> params) { return basketMapper.selectListFull(params); }
    public Basket selectDirect(Map<String, Object> params) { return basketMapper.selectDirect(params); }
    public List<Basket> selectList(Map<String, Object> params) { return basketMapper.selectList(params); }
    public Basket select(Map<String, Object> params) { return basketMapper.select(params); }
    public Basket selectByItem(Map<String, Object> params) { return basketMapper.selectByItem(params); }
    public void insert(Basket basket) { basketMapper.insert(basket); }
    public void update(Map<String, Object> params) { basketMapper.update(params); }
    public void updateCustId(Map<String, Object> params) { basketMapper.updateCustId(params); }
    public void updateMro(Map<String, Object> params) { basketMapper.updateMro(params); }
    public void delete(Map<String, Object> params) { basketMapper.delete(params); }
    public void deleteMulti(Map<String, Object> params) { basketMapper.deleteMulti(params); }
    public void clean(Map<String, Object> params) { basketMapper.clean(params); }
}
