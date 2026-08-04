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
