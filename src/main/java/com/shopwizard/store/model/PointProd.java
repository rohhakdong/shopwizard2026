package com.shopwizard.store.model;

import lombok.Data;

@Data
public class PointProd {
    private Integer pointId;
    private String chnlCode;
    private String prodCode;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
