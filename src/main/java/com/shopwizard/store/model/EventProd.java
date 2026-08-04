package com.shopwizard.store.model;

import lombok.Data;

@Data
public class EventProd {
    private String chnlCode;
    private String eventCode;
    private String prodCode;
    private Integer locType;
    private Integer prodSeq;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
