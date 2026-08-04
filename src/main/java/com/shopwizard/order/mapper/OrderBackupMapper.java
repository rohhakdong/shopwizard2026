package com.shopwizard.order.mapper;

import com.shopwizard.order.model.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderBackupMapper {

    void insertBackupStatics(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Order> findBackupList(@Param("startDate") String startDate, @Param("endDate") String endDate);

    void deleteTempOrderNo();
    void insertTempOrderNo(@Param("startDate") String startDate, @Param("endDate") String endDate);

    void deleteBackupOrderShoplinkerDuplicate();
    void insertBackupOrderShoplinker();
    void deleteBackupOrderShoplinker();

    void deleteBackupOrderPayDuplicate();
    void insertBackupOrderPay();
    void deleteBackupOrderPay();

    void deleteBackupOrderDeliFeeDuplicate();
    void insertBackupOrderDeliFee();
    void deleteBackupOrderDeliFee();

    void deleteBackupOrderChangeNoDuplicate();
    void insertBackupOrderChangeNo();
    void deleteBackupOrderChangeNo();

    void deleteBackupOrderShipDirectDuplicate();
    void insertBackupOrderShipDirect();
    void deleteBackupOrderShipDirect();

    void deleteBackupOrderShipDirectPrintDuplicate();
    void insertBackupOrderShipDirectPrint();
    void deleteBackupOrderShipDirectPrint();

    void deleteBackupOrderReturnDirectDuplicate();
    void insertBackupOrderReturnDirect();
    void deleteBackupOrderReturnDirect();

    void deleteBackupOrderDuplicate();
    void insertBackupOrder();
    void deleteBackupOrder();

    void deleteBackupOrderProdDuplicate();
    void insertBackupOrderProd();
    void deleteBackupOrderProd();
}
