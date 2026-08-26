package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderDashBoard;
import com.shopwizard.order.model.OrderDetlStats;
import com.shopwizard.order.model.OrderProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<OrderDetlStats> selectListStatisticsProd(Map<String, Object> params) { return orderProdMapper.selectListStatisticsProd(params); }
    public List<OrderDetlStats> selectListStatisticsShop(Map<String, Object> params) { return orderProdMapper.selectListStatisticsShop(params); }
    public List<OrderDetlStats> selectListStatisticsChnl(Map<String, Object> params) { return orderProdMapper.selectListStatisticsChnl(params); }
    public List<OrderDetlStats> selectListStatisticsChnlShop(Map<String, Object> params) { return orderProdMapper.selectListStatisticsChnlShop(params); }
    public List<OrderDetlStats> selectListStatisticsChnlProd(Map<String, Object> params) { return orderProdMapper.selectListStatisticsChnlProd(params); }
}
