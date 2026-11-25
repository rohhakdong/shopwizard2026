package com.shopwizard.ship.mapper;

import com.shopwizard.ship.model.PrintRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 자동발주(배송 데이터 생성·발주서) 전용 쿼리. 대상은 "원본 라인이 출고지시이고 아직 발주확인 전(ShipCheckDate NULL)인 발주서 행". */
@Mapper
public interface ShipAutoOrderMapper {

    String selectShopName(@Param("shopCode") String shopCode);

    /** 상점의 출고지(레거시: 상점 ProdDesc). 없으면 null. */
    String selectShopProdDesc(@Param("shopCode") String shopCode);

    /** 이전에 만든 사은품 행(OrderType='사은품')을 지운다 — 재실행해도 중복되지 않게. */
    int deletePendingGifts(@Param("shopCode") String shopCode);

    /** 출고지를 채우고 우편번호의 '--' 를 '-' 로 바로잡는다. */
    int updateShipPlaceAndZip(@Param("shopCode") String shopCode, @Param("shipPlace") String shipPlace,
                              @Param("actorId") String actorId, @Param("actorName") String actorName);

    List<PrintRow> selectPendingRows(@Param("shopCode") String shopCode);

    /** 수신자 그룹의 앵커 행(구성 1번)에 그룹 총 수량·금액을 남긴다. */
    int updateGiftTotals(@Param("orderNo") Integer orderNo, @Param("orderProdNo") Integer orderProdNo,
                         @Param("giftQty") int giftQty, @Param("giftAmt") long giftAmt);

    /** 앵커 행을 바탕으로 사은품 행 1개를 만든다(OrderChangeNo = 그 라인의 최대 + 1, OrderType='사은품', 원가·금액 0). */
    int insertGiftRow(@Param("orderNo") Integer orderNo, @Param("orderProdNo") Integer orderProdNo,
                      @Param("giftName") String giftName, @Param("giftQty") int giftQty,
                      @Param("registId") String registId, @Param("registName") String registName);

    int resetBundle(@Param("shopCode") String shopCode);

    /** 같은 수신자(주소+이름)에게 가는 서로 다른 주문이 2개 이상이면 BundleYn=1 — 반환값은 표시된 행 수. */
    int markBundle(@Param("shopCode") String shopCode);

    /** 수신자 그룹별 출력 순서 키(PrintSortName) — 같은 상품 구성이 발주서에서 붙어 나오게 한다. */
    int updateSortNames(@Param("shopCode") String shopCode);

    /** 자동발주 현황: 지불완료·상품 미매칭·발주단가 미매칭·출고지시·발주서 대기 건수(상점 하나). */
    Map<String, Object> selectSummary(@Param("shopCode") String shopCode);

    /**
     * 발주서 행. confirmedAt 이 없으면 "발주확인 전 대기 행"(원본 라인이 출고지시), 있으면 그 시각에 발주확인된 행(재출력).
     * 정렬은 레거시 발주서와 같다 — 출고지, 출력순서, 합배송 먼저, 수신자, 상품명. offset/limit 이 둘 다 있을 때만 쪽 나눔.
     */
    List<Map<String, Object>> selectSheetRows(@Param("shopCode") String shopCode, @Param("confirmedAt") String confirmedAt,
                                              @Param("offset") Integer offset, @Param("limit") Integer limit);

    int selectSheetCount(@Param("shopCode") String shopCode, @Param("confirmedAt") String confirmedAt);

    /** 발주서 대기 행이 걸려 있는 주문라인(발주확인 대상) — 라인당 1건, 주문번호 순. */
    List<Map<String, Object>> selectPendingLines(@Param("shopCode") String shopCode, @Param("limit") int limit);
}
