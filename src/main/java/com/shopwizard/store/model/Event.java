package com.shopwizard.store.model;

import lombok.Data;

@Data
public class Event {
    private String chnlCode;
    private String eventCode;
    private String eventName;
    private String eventDesc;
    private String eventStartDate;
    private String eventEndDate;
    private String imgName;
    private String imgLink;
    private Integer imgWidth;
    private Integer imgHeight;
    private String eventHtml;
    private String storeCode;
    private String parentEventCode;
    private Integer eventLevel;
    private Integer eventSeq;
    private Integer adultYn;
    private Integer colCnt;
    private Integer reviewYn;
    private String tmplCode;
    private Integer boardYn;
    private Integer state;
    private String remark;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
    private Integer childCount;
}
