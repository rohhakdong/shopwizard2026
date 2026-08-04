package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.GiftOptionUpdateMapper;
import com.shopwizard.ship.model.GiftOptionUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class GiftOptionUpdateService {
    private final GiftOptionUpdateMapper giftOptionUpdateMapper;
    public List<GiftOptionUpdate> selectList(Map<String, Object> params) { return giftOptionUpdateMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return giftOptionUpdateMapper.selectCount(params); }
    public GiftOptionUpdate select(Map<String, Object> params) { return giftOptionUpdateMapper.select(params); }
    public int insert(GiftOptionUpdate giftOptionUpdate) { return giftOptionUpdateMapper.insert(giftOptionUpdate); }
    public int update(GiftOptionUpdate giftOptionUpdate) { return giftOptionUpdateMapper.update(giftOptionUpdate); }
    public int delete(GiftOptionUpdate giftOptionUpdate) { return giftOptionUpdateMapper.delete(giftOptionUpdate); }
}
