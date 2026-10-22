package com.shopwizard.company.service;

import com.shopwizard.company.mapper.ShopWarehsMapper;
import com.shopwizard.company.model.ShopWarehs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopWarehsService {
    private final ShopWarehsMapper shopWarehsMapper;

    public List<ShopWarehs> getList(Map<String, Object> params) { return shopWarehsMapper.selectList(params); }
    public int getCount(Map<String, Object> params) { return shopWarehsMapper.selectCount(params); }
    public ShopWarehs get(Map<String, Object> params) { return shopWarehsMapper.select(params); }

}
