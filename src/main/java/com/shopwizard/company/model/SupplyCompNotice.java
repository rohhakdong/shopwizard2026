package com.shopwizard.company.model;

import lombok.Data;

/** 공급사 공지(shopwizard.tCmpSupplyCompNotice) — 채널·회사 구분 없는 전역 공지. */
@Data
public class SupplyCompNotice {
    private Integer noticeNo;
    private String title;
    private String cntnts;
    private String startDate;
    private String endDate;
    private Integer popupYn;
    private Integer horiznLoc;
    private Integer verticLoc;
    private Integer popupWidth;
    private Integer popupLenght;
    private String linkAddr;
    private String remark;
    private Integer state;
    private String registDate;
    private String registId;
    private String registName;
    private String changeDate;
    private String changeId;
    private String changeName;
    /** 조회에서만 채우는 게시 상태: ING 게시중 · WAIT 예정 · END 종료 */
    private String status;
}
