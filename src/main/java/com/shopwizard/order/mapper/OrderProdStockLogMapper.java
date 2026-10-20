package com.shopwizard.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/** 주문라인별 재고 차감/복원 이력(shopion.tOrdOrderProdStockLog). PK=(OrderNo, OrderProdNo, Action). */
@Mapper
public interface OrderProdStockLogMapper {

    /** 차감 이력 기록. 이미 있으면 무시(0행). */
    int insertDeduct(@Param("orderNo") Integer orderNo, @Param("orderProdNo") Integer orderProdNo,
                     @Param("prodCode") String prodCode, @Param("qty") Integer qty,
                     @Param("registId") String registId);

    /**
     * 그 라인의 차감 이력이 있을 때만, 같은 상품코드/수량으로 복원 이력을 기록한다. PK 때문에 이미 복원된
     * 라인은 다시 들어가지 않는다 — 1행이 들어갔을 때만(=최초 복원일 때만) 실제 재고를 되돌리면 된다.
     */
    int insertRestoreFromDeduct(@Param("orderNo") Integer orderNo, @Param("orderProdNo") Integer orderProdNo,
                                @Param("registId") String registId);

    /** 차감 이력의 {prodCode, qty}. 없으면 null. */
    Map<String, Object> selectDeduct(@Param("orderNo") Integer orderNo, @Param("orderProdNo") Integer orderProdNo);
}
