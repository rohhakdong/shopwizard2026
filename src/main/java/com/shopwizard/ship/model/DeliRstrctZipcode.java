package com.shopwizard.ship.model;

import lombok.Data;

/** 배송제한 우편번호(shopwizard.tShpDeliRstrctZipcodeAddr, 기본키 구분+우편번호). 구분(DeliRstrctSect)은 배송제한 규칙의 "우편번호 구분"이 가리킨다. */
@Data
public class DeliRstrctZipcode {
    private String deliRstrctSect;
    /** 하이픈 없는 숫자(5~6자리). */
    private String zipcode;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
}
