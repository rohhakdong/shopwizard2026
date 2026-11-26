package com.shopwizard.operate.model;

import lombok.Data;

/** 유지보수 요청·수정·예정일·완료 입력(용도별로 쓰는 필드만 읽는다). */
@Data
public class MantncRequest {
    private Integer mantncNo;
    private String title;
    private String content;
    private String menuName;
    /** yyyy-MM-dd */
    private String execExpectDate;
    private String execCntnts;
    private String cmpletCntnts;
}
