package com.shopwizard.store.model;

import lombok.Data;

@Data
public class Store {
    private String chnlCode;
    private String storeCode;
    private String storeName;
    private String storeDesc;
    private String parentStoreCode;
    private Integer storeLevel;
    private Integer storeSeq;
    private Integer adultYn;
    private String tmplCode;
    private Integer state;
    private String remark;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
    private Integer childCount;
    private String storeCode1;
    private String storeName1;
    private String storeCode2;
    private String storeName2;
    private String storeCode3;
    private String storeName3;
    private String storeCode4;
    private String storeName4;
}
