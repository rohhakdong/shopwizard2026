package com.shopwizard.operate.model;

import lombok.Data;

/** 공지 등록·수정 요청(쇼핑몰 공지·공급사 공지 공용). 공급사 공지는 chnlCode·compCode 를 쓰지 않는다. */
@Data
public class NoticeRequest {
    private Integer noticeNo;
    private String title;
    private String content;
    /** yyyy-MM-dd */
    private String startDate;
    private String endDate;
    private Integer popupYn;
    private Integer horiznLoc;
    private Integer verticLoc;
    private Integer popupWidth;
    private Integer popupLenght;
    private String linkAddr;
    private String chnlCode;
    private String compCode;
}
