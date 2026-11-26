package com.shopwizard.operate.model;

import lombok.Data;

/**
 * 유지보수 요청 한 건(shopwizard.tOprMantnc). 레거시의 다단계 승인 필드는 쓰지 않고 요청 → 작업 예정일 → 작업완료만 다룬다.
 * execState: recipt 요청 · exec 작업중(예정일 입력됨) · cmplet 작업완료. 날짜는 문자열로 읽는다.
 */
@Data
public class Mantnc {
    private Integer mantncNo;
    /** 대상 화면(선택 입력) */
    private String menuName;
    private String reciptId;
    private String reciptName;
    private String reciptTitle;
    /** 요청 내용 — 목록에는 싣지 않고 단건에서만 채운다 */
    private String reciptCntnts;
    private String reciptDate;
    private String execState;
    /** 작업 예정일(yyyy-MM-dd) */
    private String execExpectDate;
    /** 작업 메모 */
    private String execCntnts;
    private String execDate;
    private String execId;
    private String execName;
    private String cmpletCntnts;
    private String cmpletDate;
    private String cmpletId;
    private String cmpletName;
    /** 미완료인데 작업 예정일이 지났으면 1 */
    private Integer overdue;
}
