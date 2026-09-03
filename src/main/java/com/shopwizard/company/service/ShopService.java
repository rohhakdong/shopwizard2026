package com.shopwizard.company.service;

import com.shopwizard.company.mapper.ShopMapper;
import com.shopwizard.company.model.Shop;
import com.shopwizard.company.model.ShopPublicView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopService {
    private final ShopMapper shopMapper;

    public List<Shop> getList(Map<String, Object> params) { return shopMapper.selectList(params); }
    public List<ShopPublicView> getPublicList() { return shopMapper.selectPublicList(); }
    public int getCount(Map<String, Object> params) { return shopMapper.selectCount(params); }
    public String getMax(String supplyCode) { return shopMapper.selectMax(supplyCode); }
    public Shop get(String shopCode) { return shopMapper.select(shopCode); }
    public Shop getByName(String shopName) { return shopMapper.selectByName(shopName); }
    public int getLoginIdCount(Map<String, Object> params) { return shopMapper.selectLoginIdCount(params); }

    @Transactional
    public int insert(Shop shop) { return shopMapper.insert(shop); }
    @Transactional
    public int update(Shop shop) { return shopMapper.update(shop); }
    @Transactional
    public int delete(Shop shop) { return shopMapper.delete(shop); }
}
