package com.shopwizard.order.mapper;

import com.shopwizard.order.model.OrderDashBoard;
import com.shopwizard.order.model.OrderDetlStats;
import com.shopwizard.order.model.OrderProd;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderProdMapper {
    List<OrderProd> selectList(Map<String, Object> params);
    int selectListCount(Map<String, Object> params);
    OrderProd select(Map<String, Object> params);

    /** 상품 미매칭(ProdCode NULL) 주문라인을 (상품명, 옵션) 단위로 묶은 목록. 상품 매칭 화면용. */
    List<Map<String, Object>> selectListUnmatched(Map<String, Object> params);
    int selectListUnmatchedCount(Map<String, Object> params);
    /** 특정 (상품명, 옵션) 의 미매칭 라인들 (backfill 대상). */
    List<OrderProd> selectUnmatchedLinesByName(Map<String, Object> params);
    /** 조인 없이 주문상품 라인 1건 원본 조회. */
    OrderProd selectLineRaw(Map<String, Object> params);
    /** selectLineRaw 의 잠금 변형(FOR UPDATE) — 상태 전이 서비스가 확인과 갱신 사이에 다른 요청이 끼어들지 못하게 쓴다. */
    OrderProd selectLineForUpdate(Map<String, Object> params);

    List<Integer> selectOrderProdNoList(Integer orderNo);
    List<OrderProd> selectGuestOrderList(Map<String, Object> params);
    void insert(OrderProd orderProd);
    void update(OrderProd orderProd);
    void updatePrice(OrderProd orderProd);
    void updatePriceShipDirect(OrderProd orderProd);
    void updatePriceShipDirectPrint(OrderProd orderProd);
    void updatePriceAdjustDetail(OrderProd orderProd);
    void delete(Map<String, Object> params);
    List<OrderProd> mypageSelectList(Map<String, Object> params);
    void mypageUpdate(Map<String, Object> params);
    int countByCustId(Integer custId);
    int selectMonthlyTotalAmt(Integer custId);
    int selectMonthlyTotalAmtDeptAccnt(Map<String, Object> params);
    List<OrderDashBoard> selectListDashboardAnyb(Map<String, Object> params);
    void updateReturnReason(OrderProd orderProd);
    void updateOrderState(Map<String, Object> params);
    /** 배송 전 상태의 라인만 주문취소로 바꾼다. 영향 행수(바뀌었으면 1) 반환. */
    int cancelByWebhook(Map<String, Object> params);
    void updateOrderStateError(Map<String, Object> params);
    void updateRefundCancel(Map<String, Object> params);
    void updateShopInfo(Map<String, Object> params);
    void updateInvoice(Map<String, Object> params);
    void updateCustConsltYn(Map<String, Object> params);
    int countOrderStateDiffer(Map<String, Object> params);
    /** 주문에서 이미 환불완료된 라인의 금액 합(라인 ReciptAmt, 없으면 판매가×수량) — PG 잔액 대조용. */
    long sumRefundedAmount(int orderNo);
    /** 서비스 범위의 라인 수를 상태별로 센 결과 — 각 행은 {state, cnt}. 대시보드 "배송 대기"용. */
    List<Map<String, Object>> countByStates(Map<String, Object> params);
    List<OrderDetlStats> selectListStatisticsProd(Map<String, Object> params);
    List<OrderDetlStats> selectListStatisticsShop(Map<String, Object> params);
    List<OrderDetlStats> selectListStatisticsChnl(Map<String, Object> params);
    List<OrderDetlStats> selectListStatisticsChnlShop(Map<String, Object> params);
    List<OrderDetlStats> selectListStatisticsChnlProd(Map<String, Object> params);
}
