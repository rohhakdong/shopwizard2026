package com.shopwizard.ship.mapper;

import com.shopwizard.ship.model.GiftOptionUpdateDetail;
import com.shopwizard.ship.model.ShipDirect;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface GiftOptionUpdateDetailMapper {
    List<GiftOptionUpdateDetail> selectList(Map<String, Object> params);
    int selectCount(Map<String, Object> params);
    GiftOptionUpdateDetail select(Map<String, Object> params);
    int insert(GiftOptionUpdateDetail giftOptionUpdateDetail);
    int update(GiftOptionUpdateDetail giftOptionUpdateDetail);
    int delete(GiftOptionUpdateDetail giftOptionUpdateDetail);
    List<ShipDirect> selectShipDirectList(Map<String, Object> params);
    List<ShipDirect> selectShipDirectPrintList(Map<String, Object> params);
    int updateShipDirectPrint(Map<String, Object> params);
    int updateShipDirectPrintAutionno(Map<String, Object> params);
}
