package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderDashBoard;
import com.shopwizard.order.model.OrderDetlStats;
import com.shopwizard.order.model.OrderProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderProdService {
    private final OrderProdMapper orderProdMapper;

    public List<OrderProd> selectList(Map<String, Object> params) { return orderProdMapper.selectList(params); }
    public int selectListCount(Map<String, Object> params) { return orderProdMapper.selectListCount(params); }
    public OrderProd select(Map<String, Object> params) { return orderProdMapper.select(params); }
    public List<Integer> selectOrderProdNoList(Integer orderNo) { return orderProdMapper.selectOrderProdNoList(orderNo); }
    public List<OrderProd> selectGuestOrderList(Map<String, Object> params) { return orderProdMapper.selectGuestOrderList(params); }
    public void insert(OrderProd orderProd) { orderProdMapper.insert(orderProd); }
    public void update(OrderProd orderProd) { orderProdMapper.update(orderProd); }
    public void updatePrice(OrderProd orderProd) { orderProdMapper.updatePrice(orderProd); }
    public void updatePriceShipDirect(OrderProd orderProd) { orderProdMapper.updatePriceShipDirect(orderProd); }
    public void updatePriceShipDirectPrint(OrderProd orderProd) { orderProdMapper.updatePriceShipDirectPrint(orderProd); }
    public void updatePriceAdjustDetail(OrderProd orderProd) { orderProdMapper.updatePriceAdjustDetail(orderProd); }
    public void delete(Map<String, Object> params) { orderProdMapper.delete(params); }
    public List<OrderProd> mypageSelectList(Map<String, Object> params) { return orderProdMapper.mypageSelectList(params); }
    public void mypageUpdate(Map<String, Object> params) { orderProdMapper.mypageUpdate(params); }
    public int countByCustId(Integer custId) { return orderProdMapper.countByCustId(custId); }
    public int selectMonthlyTotalAmt(Integer custId) { return orderProdMapper.selectMonthlyTotalAmt(custId); }
    public int selectMonthlyTotalAmtDeptAccnt(Map<String, Object> params) { return orderProdMapper.selectMonthlyTotalAmtDeptAccnt(params); }
    public List<OrderDashBoard> selectListDashboardAnyb(Map<String, Object> params) { return orderProdMapper.selectListDashboardAnyb(params); }
    public void updateReturnReason(OrderProd orderProd) { orderProdMapper.updateReturnReason(orderProd); }
    public void updateOrderState(Map<String, Object> params) { orderProdMapper.updateOrderState(params); }
    public void updateOrderStateError(Map<String, Object> params) { orderProdMapper.updateOrderStateError(params); }
    public void updateRefundCancel(Map<String, Object> params) { orderProdMapper.updateRefundCancel(params); }
    public void updateShopInfo(Map<String, Object> params) { orderProdMapper.updateShopInfo(params); }
    public void updateInvoice(Map<String, Object> params) { orderProdMapper.updateInvoice(params); }
    public void updateCustConsltYn(Map<String, Object> params) { orderProdMapper.updateCustConsltYn(params); }
    public int countOrderStateDiffer(Map<String, Object> params) { return orderProdMapper.countOrderStateDiffer(params); }
    public int cancelByWebhook(Map<String, Object> params) { return orderProdMapper.cancelByWebhook(params); }

    /** 배송이 시작되기 전 단계(대시보드 "배송 대기"에 쓰는 상태들). 순서는 업무 흐름 순서. */
    static final List<String> PENDING_SHIP_STATES = List.of("지불완료", "출고지시", "발주확인");

    /**
     * 서비스(svcCode) 범위의 배송 전 단계 라인 수를 상태별로 돌려준다. 결과에는 항상 세 상태가 모두 들어 있고
     * (해당 라인이 없으면 0), 세 상태 외의 값은 쿼리에서 걸러진다. svcCode 가 비어 있으면 조회하지 않고 모두 0.
     */
    public Map<String, Integer> selectPendingShipCounts(String svcCode) {
        return selectPendingShipCounts(svcCode, com.shopwizard.framework.auth.DataScope.UNRESTRICTED);
    }

    /** 상점/채널로 제한된 관리자는 자기 상점·채널 라인만 센다(대시보드가 다른 업체의 물량을 보여주지 않도록). */
    public Map<String, Integer> selectPendingShipCounts(String svcCode, com.shopwizard.framework.auth.DataScope scope) {
        Map<String, Integer> result = new LinkedHashMap<>();
        PENDING_SHIP_STATES.forEach(s -> result.put(s, 0));
        if (svcCode == null || svcCode.isBlank()) return result;

        Map<String, Object> params = new HashMap<>();
        params.put("svcCode", svcCode);
        params.put("states", PENDING_SHIP_STATES);
        if (scope != null) {
            params.put("shopCode", scope.shopCode());
            params.put("chnlCode", scope.chnlCode());
        }
        for (Map<String, Object> row : orderProdMapper.countByStates(params)) {
            String state = row.get("state") == null ? null : String.valueOf(row.get("state"));
            if (state != null && result.containsKey(state) && row.get("cnt") instanceof Number n) {
                result.put(state, n.intValue());
            }
        }
        return result;
    }
    public List<OrderDetlStats> selectListStatisticsProd(Map<String, Object> params) { return orderProdMapper.selectListStatisticsProd(params); }
    public List<OrderDetlStats> selectListStatisticsShop(Map<String, Object> params) { return orderProdMapper.selectListStatisticsShop(params); }
    public List<OrderDetlStats> selectListStatisticsChnl(Map<String, Object> params) { return orderProdMapper.selectListStatisticsChnl(params); }
    public List<OrderDetlStats> selectListStatisticsChnlShop(Map<String, Object> params) { return orderProdMapper.selectListStatisticsChnlShop(params); }
    public List<OrderDetlStats> selectListStatisticsChnlProd(Map<String, Object> params) { return orderProdMapper.selectListStatisticsChnlProd(params); }
}
