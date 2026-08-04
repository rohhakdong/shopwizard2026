package com.shopwizard.company.model;

import lombok.Data;

@Data
public class SupplyCompNotice {
    private int noticeNo;
    private String title;
    private String cntnts;
    private String startDate;
    private String endDate;
    private int popupYn;
    private int horiznLoc;
    private int verticLoc;
    private int popupWidth;
    private int popupLenght;
    private String linkAddr;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
}
