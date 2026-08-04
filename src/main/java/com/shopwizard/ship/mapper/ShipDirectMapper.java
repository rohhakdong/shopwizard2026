package com.shopwizard.ship.mapper;

import com.shopwizard.ship.model.ShipDirect;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ShipDirectMapper {
    List<ShipDirect> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    ShipDirect select(Map<String, Object> params);
    ShipDirect selectSimple(Map<String, Object> params);
    List<ShipDirect> selectListShipPlace(Map<String, Object> params);
    List<ShipDirect> selectListPrint2(Map<String, Object> params);
    int selectCountPrint2(Map<String, Object> params);
    int insert(ShipDirect shipDirect);
    int insertShipDirect(Map<String, Object> params);
    int insertShipDirectPrint(Map<String, Object> params);
    int insertShipDirectPrintByMatching(Map<String, Object> params);
    int checkShipDirectPrintByMatchingSocial(Map<String, Object> params);
    int insertShipDirectPrintByMatchingSocial(Map<String, Object> params);
    int insertShipDirectPrintWithGift(Map<String, Object> params);
    int update(Map<String, Object> params);
    int updatePrint(Map<String, Object> params);
    int updatePrintSortName(Map<String, Object> params);
    int updateRefundCancel(Map<String, Object> params);
    int updateInvoice(Map<String, Object> params);
    int updateAdjustSelectDate(Map<String, Object> params);
    int updateAdjustSelectDateSchedule(Map<String, Object> params);
    int countInvoice(Map<String, Object> params);
    int delete(ShipDirect shipDirect);
    List<ShipDirect> selectCmpletList(Map<String, Object> params);
    int selectCmpletCount(Map<String, Object> params);
    List<ShipDirect> selectCmpletListByOrder(Map<String, Object> params);
    int selectCmpletCountByOrder(Map<String, Object> params);
}
