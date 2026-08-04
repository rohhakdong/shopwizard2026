package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.ShipDirectMapper;
import com.shopwizard.ship.model.ShipDirect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipDirectService {
    private final ShipDirectMapper shipDirectMapper;
    public List<ShipDirect> selectList(Map<String, Object> params) { return shipDirectMapper.selectList(params); }
    public int selectCount(Map<String, Object> params) { return shipDirectMapper.selectCount(params); }
    public ShipDirect select(Map<String, Object> params) { return shipDirectMapper.select(params); }
    public ShipDirect selectSimple(Map<String, Object> params) { return shipDirectMapper.selectSimple(params); }
    public List<ShipDirect> selectListShipPlace(Map<String, Object> params) { return shipDirectMapper.selectListShipPlace(params); }
    public List<ShipDirect> selectListPrint2(Map<String, Object> params) { return shipDirectMapper.selectListPrint2(params); }
    public int selectCountPrint2(Map<String, Object> params) { return shipDirectMapper.selectCountPrint2(params); }
    public int insert(ShipDirect shipDirect) { return shipDirectMapper.insert(shipDirect); }
    public int insertShipDirect(Map<String, Object> params) { return shipDirectMapper.insertShipDirect(params); }
    public int insertShipDirectPrint(Map<String, Object> params) { return shipDirectMapper.insertShipDirectPrint(params); }
    public int insertShipDirectPrintByMatching(Map<String, Object> params) { return shipDirectMapper.insertShipDirectPrintByMatching(params); }
    public int checkShipDirectPrintByMatchingSocial(Map<String, Object> params) { return shipDirectMapper.checkShipDirectPrintByMatchingSocial(params); }
    public int insertShipDirectPrintByMatchingSocial(Map<String, Object> params) { return shipDirectMapper.insertShipDirectPrintByMatchingSocial(params); }
    public int insertShipDirectPrintWithGift(Map<String, Object> params) { return shipDirectMapper.insertShipDirectPrintWithGift(params); }
    public int update(Map<String, Object> params) { return shipDirectMapper.update(params); }
    public int updatePrint(Map<String, Object> params) { return shipDirectMapper.updatePrint(params); }
    public int updatePrintSortName(Map<String, Object> params) { return shipDirectMapper.updatePrintSortName(params); }
    public int updateRefundCancel(Map<String, Object> params) { return shipDirectMapper.updateRefundCancel(params); }
    public int updateInvoice(Map<String, Object> params) { return shipDirectMapper.updateInvoice(params); }
    public int updateAdjustSelectDate(Map<String, Object> params) { return shipDirectMapper.updateAdjustSelectDate(params); }
    public int updateAdjustSelectDateSchedule(Map<String, Object> params) { return shipDirectMapper.updateAdjustSelectDateSchedule(params); }
    public int countInvoice(Map<String, Object> params) { return shipDirectMapper.countInvoice(params); }
    public int delete(ShipDirect shipDirect) { return shipDirectMapper.delete(shipDirect); }
    public List<ShipDirect> selectCmpletList(Map<String, Object> params) { return shipDirectMapper.selectCmpletList(params); }
    public int selectCmpletCount(Map<String, Object> params) { return shipDirectMapper.selectCmpletCount(params); }
    public List<ShipDirect> selectCmpletListByOrder(Map<String, Object> params) { return shipDirectMapper.selectCmpletListByOrder(params); }
    public int selectCmpletCountByOrder(Map<String, Object> params) { return shipDirectMapper.selectCmpletCountByOrder(params); }
}
