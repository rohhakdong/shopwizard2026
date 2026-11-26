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

    /** 발주내역2생성: 상점·발주일 범위의 대상 주문번호(중복 없이, 번호순). */
    List<Integer> selectRunOrderNos(@org.apache.ibatis.annotations.Param("shopCode") String shopCode,
                                    @org.apache.ibatis.annotations.Param("startDate") String startDate,
                                    @org.apache.ibatis.annotations.Param("endDate") String endDate);

    /** 발주내역2생성: 샵링커 채널상품번호를 상점·발주일 범위 발주서 행에 채운다. */
    int updatePrintAuctionnoByShop(@org.apache.ibatis.annotations.Param("shopCode") String shopCode,
                                   @org.apache.ibatis.annotations.Param("startDate") String startDate,
                                   @org.apache.ibatis.annotations.Param("endDate") String endDate);
}
