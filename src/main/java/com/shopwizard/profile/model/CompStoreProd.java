package com.shopwizard.profile.model;

import lombok.Data;

@Data
public class CompStoreProd {
    private String compCode;
    private String storeCode;
    private String prodCode;
    private Integer locType;
    private Integer prodSeq;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
