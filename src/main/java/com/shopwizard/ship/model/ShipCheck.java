package com.shopwizard.ship.model;

import lombok.Data;

@Data
public class ShipCheck {
    private Integer shipCheckNo;
    private String shopCode;
    /** 조회 전용(tCmpShop 조인). */
    private String shopName;
    private String shipCheckDesc;
    private String shipCheckDate;
    private Integer orderCount;
    private Integer printCount;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
