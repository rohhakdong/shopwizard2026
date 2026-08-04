package com.shopwizard.order.model;

import lombok.Data;

@Data
public class OrderDashBoard {
    private String shopCode;
    private String shopName;
    private Integer orderReceiptCount;
    private Integer payCmpletCount;
    private Integer shipDirectCount;
    private Integer shipCheckCount;
    private Integer shipCmpletCount;
    private Integer longDelayCount;
    private Integer longReturnCount;
}
