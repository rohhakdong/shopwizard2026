package com.shopwizard.catalog.model;

import lombok.Data;

import java.util.List;

/** 샵링커 전송 요청 — 보낼 상품코드 목록. 등록자 같은 감사값은 본문이 아니라 세션에서 읽는다. */
@Data
public class SlinkSendRequest {
    private List<String> prodCodes;
}
