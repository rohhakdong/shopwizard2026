package com.shopwizard.order.model;

import lombok.Data;

/**
 * 샵링커 주문수집 실행 이력의 건별 결과 (테이블 {@code shopion.tOrdSlnkTransDetail}).
 * 수집한 주문 1건당 1행. {@code resultType} = 성공 / 중복 / 무시 / 오류.
 */
@Data
public class SlnkTransDetail {
    private Integer transId;
    private Integer transDetailNo;
    private String slnkOrderCode;   // 샵링커 주문코드
    private String resultType;
    private String resultMessag;
    private Integer state;
    private String remark;
    private String registId;
    private String registName;
    private String registDate;
    private String changeId;
    private String changeName;
    private String changeDate;
}
