package com.shopwizard.order.model;

import lombok.Data;

import java.util.List;

/**
 * 관리자가 주문관리 화면에서 직접 등록하는 주문(오픈마켓·전화 주문을 옮겨 적는 용도). 주문은 항상 "지불완료"로 만들어지고
 * 결제 행은 "수기"다. 상품 줄에서는 상품코드·옵션코드·수량만 필수이고, 판매가·옵션가는 화면에서 고친 값이 있으면 그 값(단가)을 쓴다.
 * 공급가·매입가는 받지 않고 상품 마스터에서 가져온다.
 */
@Data
public class ManualOrderRequest {
    private String chnlCode;
    /** 채널(오픈마켓 등)의 주문번호 — 선택. 있으면 같은 채널에서 두 번 등록할 수 없다. */
    private String chnlOrderNo;
    /** 주문일자(yyyy-MM-dd) — 선택, 비면 지금. */
    private String orderReciptDate;

    private String orderName;
    private String orderPhoneNo;
    private String orderMobileNo;
    private String orderEmail;

    private String recverName;
    private String recverPhoneNo;
    private String recverMobileNo;
    private String recverZipcode;
    private String recverAddr1;
    private String recverAddr2;
    private String deliMemo;

    private List<Line> lines;

    /** 최상위 감사값 — 서버가 세션 값으로 덮어쓴다(ActorAuditAdvice). */
    private String registId;
    private String registName;

    @Data
    public static class Line {
        private String prodCode;
        private String itemCode;
        private Integer prodQty;
        /** 상품 판매가(단가) — 비우면 상품 현재 판매가. */
        private Integer salePrice;
        /** 옵션 추가금(단가) — 비우면 옵션의 현재 추가금. */
        private Integer optionPrice;
    }
}
