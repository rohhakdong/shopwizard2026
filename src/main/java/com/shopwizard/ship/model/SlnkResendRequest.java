package com.shopwizard.ship.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 샵링커 송장 재전송 요청 — 다시 보낼 줄 목록. */
@Data
public class SlnkResendRequest {
    private List<Line> lines = new ArrayList<>();

    @Data
    public static class Line {
        private Integer orderNo;
        private Integer orderProdNo;
        private Integer orderChangeNo;
    }
}
