package com.shopwizard.store.model;

import lombok.Data;

@Data
public class EventBoard {
    private String chnlCode;
    private Integer boardNo;
    private String boardTitle;
    private String boardCntnts;
    private String eventCode;
    private String eventName;
    private Integer custId;
    private String custName;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
