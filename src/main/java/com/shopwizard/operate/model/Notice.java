package com.shopwizard.operate.model;

import lombok.Data;

@Data
public class Notice {
    private Integer noticeNo;
    private String noticeTitle;
    private String noticeCntnts;
    private String startDate;
    private String endDate;
    private Integer popupYn;
    private String storeCode;
    private String eventCode;
    private String chnlCode;
    private String compCode;
    private Integer horiznLoc;
    private Integer verticLoc;
    private Integer popupWidth;
    private Integer popupLenght;
    private String linkAddr;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
