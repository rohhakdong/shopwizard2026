package com.shopwizard.operate.model;

import lombok.Data;

@Data
public class CustConsltDashBoard {
    private String chnlCode;
    private String chnlName;
    private Integer todayConsltCount;
    private Integer todayAnswrCount;
    private Integer noAnswrCount;
    private Integer longDelayCount;
}
