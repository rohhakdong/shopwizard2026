package com.shopwizard.ship.model;

import lombok.Data;

/**
 * 발주서(tShpShipDirectPrint) 행 중 배송 데이터 생성(묶음·사은품 계산)에 필요한 값만 담는다.
 * 새 시스템의 키 규약: OrderProdNo = 원래 주문 라인번호, OrderChangeNo = 구성 순번(1..N, 사은품은 그 뒤 번호).
 */
@Data
public class PrintRow {
    private Integer orderNo;
    private Integer orderProdNo;
    private Integer orderChangeNo;
    private String orderType;
    private String chnlCode;
    private String dealName;
    /** 채널 원본 상품명(사은품 규칙의 채널상품명 조건 대상). */
    private String orderProdName;
    private String prodName;
    private Integer prodQty;
    /** 원본 주문 라인 수량(구성 분해 전). */
    private Integer orderProdQty;
    private Integer salePrice;
    private String recverAddr;
    private String recverName;
    /** 결제 완료 일시(yyyy-MM-dd HH:mm:ss) — 사은품 증정 기간 판정에 쓴다. */
    private String payCmpletDate;
}
