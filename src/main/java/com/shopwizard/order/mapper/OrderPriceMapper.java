package com.shopwizard.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 주문 엑셀 가격수정 — 줄의 가격을 고치면서 출고 사본·미마감 정산자료·반품 사본을 같은 트랜잭션에서 맞춘다.
 * 기존 {@code OrderProdMapper.updatePriceShipDirect/updatePriceAdjustDetail} 은 OrderChangeNo·부호·정산마감 가드가 없어 쓰지 않는다.
 */
@Mapper
public interface OrderPriceMapper {

    /**
     * 줄 하나의 수정 가능 여부를 정하는 정보 — 키 {@code payType}(1번 결제의 PayType), {@code closedCnt}(마감된 정산자료 수),
     * {@code openCnt}(마감 전 정산자료 수), {@code shipCopyCnt}(출고 사본 수), {@code returnCopyCnt}(반품 사본 수).
     */
    Map<String, Object> selectGuard(@Param("orderNo") int orderNo, @Param("orderProdNo") int orderProdNo);

    /** 주문 줄(tOrdOrderProd) — 상태가 아직 같을 때만(expectedState). 반영된 행 수. */
    int updateLine(Map<String, Object> p);

    /** 출고 사본(tShpShipDirect) — 수량이 같은 행만. */
    int updateShipCopy(Map<String, Object> p);

    /** 반품 사본(tShpReturnDirect) — 수량이 같은 행만. */
    int updateReturnCopy(Map<String, Object> p);

    /** 마감 전 출고 정산자료(수량 +) */
    int updateOpenShipDetail(Map<String, Object> p);

    /** 마감 전 반품 정산자료(수량 −, 금액은 부호 반전) */
    int updateOpenReturnDetail(Map<String, Object> p);
}
