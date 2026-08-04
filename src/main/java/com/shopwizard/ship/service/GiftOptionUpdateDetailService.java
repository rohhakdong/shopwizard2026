package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.GiftOptionUpdateDetailMapper;
import com.shopwizard.ship.model.GiftOptionUpdateDetail;
import com.shopwizard.ship.model.ShipDirect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class GiftOptionUpdateDetailService {
    private final GiftOptionUpdateDetailMapper giftOptionUpdateDetailMapper;
    public List<GiftOptionUpdateDetail> selectList(Map<String, Object> params) { return giftOptionUpdateDetailMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return giftOptionUpdateDetailMapper.selectCount(params); }
    public GiftOptionUpdateDetail select(Map<String, Object> params) { return giftOptionUpdateDetailMapper.select(params); }
    public int insert(GiftOptionUpdateDetail giftOptionUpdateDetail) { return giftOptionUpdateDetailMapper.insert(giftOptionUpdateDetail); }
    public int update(GiftOptionUpdateDetail giftOptionUpdateDetail) { return giftOptionUpdateDetailMapper.update(giftOptionUpdateDetail); }
    public int delete(GiftOptionUpdateDetail giftOptionUpdateDetail) { return giftOptionUpdateDetailMapper.delete(giftOptionUpdateDetail); }
    public List<ShipDirect> selectShipDirectList(Map<String, Object> params) { return giftOptionUpdateDetailMapper.selectShipDirectList(params); }
    public List<ShipDirect> selectShipDirectPrintList(Map<String, Object> params) { return giftOptionUpdateDetailMapper.selectShipDirectPrintList(params); }
    public int updateShipDirectPrint(Map<String, Object> params) { return giftOptionUpdateDetailMapper.updateShipDirectPrint(params); }
    public int updateShipDirectPrintAutionno(Map<String, Object> params) { return giftOptionUpdateDetailMapper.updateShipDirectPrintAutionno(params); }
}
