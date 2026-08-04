package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderBackupMapper;
import com.shopwizard.order.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderBackupService {

    private final OrderBackupMapper orderBackupMapper;

    public void insertBackupStatics(String startDate, String endDate) {
        orderBackupMapper.insertBackupStatics(startDate, endDate);
    }

    public List<Order> findBackupList(String startDate, String endDate) {
        return orderBackupMapper.findBackupList(startDate, endDate);
    }

    public void deleteTempOrderNo() { orderBackupMapper.deleteTempOrderNo(); }
    public void insertTempOrderNo(String startDate, String endDate) { orderBackupMapper.insertTempOrderNo(startDate, endDate); }

    public void deleteBackupOrderShoplinkerDuplicate() { orderBackupMapper.deleteBackupOrderShoplinkerDuplicate(); }
    public void insertBackupOrderShoplinker() { orderBackupMapper.insertBackupOrderShoplinker(); }
    public void deleteBackupOrderShoplinker() { orderBackupMapper.deleteBackupOrderShoplinker(); }

    public void deleteBackupOrderPayDuplicate() { orderBackupMapper.deleteBackupOrderPayDuplicate(); }
    public void insertBackupOrderPay() { orderBackupMapper.insertBackupOrderPay(); }
    public void deleteBackupOrderPay() { orderBackupMapper.deleteBackupOrderPay(); }

    public void deleteBackupOrderDeliFeeDuplicate() { orderBackupMapper.deleteBackupOrderDeliFeeDuplicate(); }
    public void insertBackupOrderDeliFee() { orderBackupMapper.insertBackupOrderDeliFee(); }
    public void deleteBackupOrderDeliFee() { orderBackupMapper.deleteBackupOrderDeliFee(); }

    public void deleteBackupOrderChangeNoDuplicate() { orderBackupMapper.deleteBackupOrderChangeNoDuplicate(); }
    public void insertBackupOrderChangeNo() { orderBackupMapper.insertBackupOrderChangeNo(); }
    public void deleteBackupOrderChangeNo() { orderBackupMapper.deleteBackupOrderChangeNo(); }

    public void deleteBackupOrderShipDirectDuplicate() { orderBackupMapper.deleteBackupOrderShipDirectDuplicate(); }
    public void insertBackupOrderShipDirect() { orderBackupMapper.insertBackupOrderShipDirect(); }
    public void deleteBackupOrderShipDirect() { orderBackupMapper.deleteBackupOrderShipDirect(); }

    public void deleteBackupOrderShipDirectPrintDuplicate() { orderBackupMapper.deleteBackupOrderShipDirectPrintDuplicate(); }
    public void insertBackupOrderShipDirectPrint() { orderBackupMapper.insertBackupOrderShipDirectPrint(); }
    public void deleteBackupOrderShipDirectPrint() { orderBackupMapper.deleteBackupOrderShipDirectPrint(); }

    public void deleteBackupOrderReturnDirectDuplicate() { orderBackupMapper.deleteBackupOrderReturnDirectDuplicate(); }
    public void insertBackupOrderReturnDirect() { orderBackupMapper.insertBackupOrderReturnDirect(); }
    public void deleteBackupOrderReturnDirect() { orderBackupMapper.deleteBackupOrderReturnDirect(); }

    public void deleteBackupOrderDuplicate() { orderBackupMapper.deleteBackupOrderDuplicate(); }
    public void insertBackupOrder() { orderBackupMapper.insertBackupOrder(); }
    public void deleteBackupOrder() { orderBackupMapper.deleteBackupOrder(); }

    public void deleteBackupOrderProdDuplicate() { orderBackupMapper.deleteBackupOrderProdDuplicate(); }
    public void insertBackupOrderProd() { orderBackupMapper.insertBackupOrderProd(); }
    public void deleteBackupOrderProd() { orderBackupMapper.deleteBackupOrderProd(); }
}
