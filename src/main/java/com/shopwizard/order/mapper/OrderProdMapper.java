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
    void updateOrderStateError(Map<String, Object> params);
    void updateRefundCancel(Map<String, Object> params);
    void updateShopInfo(Map<String, Object> params);
    void updateInvoice(Map<String, Object> params);
    void updateCustConsltYn(Map<String, Object> params);
    int countOrderStateDiffer(Map<String, Object> params);
    List<OrderDetlStats> selectListStatisticsProd(Map<String, Object> params);
    List<OrderDetlStats> selectListStatisticsShop(Map<String, Object> params);
    List<OrderDetlStats> selectListStatisticsChnl(Map<String, Object> params);
    List<OrderDetlStats> selectListStatisticsChnlShop(Map<String, Object> params);
    List<OrderDetlStats> selectListStatisticsChnlProd(Map<String, Object> params);
}
