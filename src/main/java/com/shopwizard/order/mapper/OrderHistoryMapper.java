package com.shopwizard.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 주문상세의 배송·반품·결제 내역(읽기 전용). 한 주문의 출고 사본·반품 사본을 <b>전부</b> 돌려준다 — 목록 화면용 쿼리는 취소·미출고 행을
 * 걸러내지만 상세에서는 이력이 모두 보여야 한다. 날짜는 문자열로, 카드번호 같은 민감 컬럼은 읽지 않는다.
 */
@Mapper
public interface OrderHistoryMapper {

    /** 출고 사본(tShpShipDirect) + 택배사(이름·조회 URL). */
    List<Map<String, Object>> selectShips(@Param("orderNo") int orderNo);

    /** 반품 사본(tShpReturnDirect) + 택배사. */
    List<Map<String, Object>> selectReturns(@Param("orderNo") int orderNo);

    /** 결제(tOrdOrderPay) + 결제수단 이름. */
    List<Map<String, Object>> selectPays(@Param("orderNo") int orderNo);
}
