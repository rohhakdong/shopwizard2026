package com.shopwizard.ship.model;

import lombok.Data;

@Data
public class ShipCmplet {
    private Integer shipNo;
    private Integer orderNo;
    private Integer orderProdNo;
    private Integer orderChangeNo;
    private String shipCmpletDate;
    private String barcodNo;
    private String execMessag;
    private String adjustSelectDate;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
