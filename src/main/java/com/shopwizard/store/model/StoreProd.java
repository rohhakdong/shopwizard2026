package com.shopwizard.store.model;

import lombok.Data;

@Data
public class StoreProd {
    private String chnlCode;
    private String storeCode;
    private String prodCode;
    private Integer locType;
    private Integer prodSeq;
    private Integer orderQty;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
