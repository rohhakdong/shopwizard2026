package com.shopwizard.ship.model;

import lombok.Data;

@Data
public class GiftOptionUpdateDetail {
    private Integer updateId;
    private Integer updateDetailNo;
    private Integer orderNo;
    private String resultType;
    private String resultMessag;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
