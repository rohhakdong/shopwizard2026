package com.shopwizard.stock.model;

import lombok.Data;

@Data
public class DailyStockState {
    private String stockDate;
    private String orderName;
    private String warehsCode;
    private String barCode;
    private String stockName;
    private Integer stockCost;
    private Integer boxQty;
    private Integer carryQty;
    private Integer enterQty;
    private Integer shipQty;
    private Integer reEnterQty;
    private Integer brokenQty;
    private Integer badQty;
    private Integer corrctQty;
    private Integer stockQty;
    private Integer stockAmt;
    private String makeDate;
    private String expireDate;
    private Integer accumBrokenQty;
    private Integer accumShipQty;
    private String stockDesc;
    private String shopCode;
    private Integer shipCheckQty;
    private Integer reEnterCheckQty;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
