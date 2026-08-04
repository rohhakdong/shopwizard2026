package com.shopwizard.store.model;

import lombok.Data;

@Data
public class Design {
    private Integer designId;
    private String designName;
    private String designDesc;
    private String svcCode;
    private Integer state;
    private String remark;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
