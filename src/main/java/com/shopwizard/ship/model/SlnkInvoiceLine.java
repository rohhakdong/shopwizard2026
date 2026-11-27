package com.shopwizard.ship.model;

import lombok.Data;

/** 샵링커 송장 전송 대상 줄(shopwizard.tShpShipDirect 의 한 행) — 전송에 필요한 값만. */
@Data
public class SlnkInvoiceLine {
    private Integer orderNo;
    private Integer orderProdNo;
    private Integer orderChangeNo;
    private String orderState;
    private String shopCode;
    private String chnlCode;
    private String chnlName;
    /** 샵링커 주문코드(수집 때 저장) — 없으면 샵링커 주문이 아니다. */
    private String slnkOrderCode;
    private String deliCompName;
    /** 택배사 마스터의 "샵링커 택배사명"(비면 deliCompName 을 보낸다). */
    private String slnkDeliName;
    private String invNo;
    private String shipCmpletDate;
    private String orderName;
    private String recverName;
}
