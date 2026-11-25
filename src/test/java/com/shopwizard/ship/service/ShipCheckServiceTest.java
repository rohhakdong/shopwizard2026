package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.ShipCheckMapper;
import com.shopwizard.ship.model.ShipCheck;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 발주 목록 헤더 — 이름 형식(레거시와 동일)과 상점별 1건 기록. */
class ShipCheckServiceTest {

    private final ShipCheckMapper mapper = mock(ShipCheckMapper.class);
    private final ShipCheckService service = new ShipCheckService(mapper);

    @Test
    void 발주_이름은_레거시_형식_상점명_날짜_오전_오후_시각이다() {
        assertThat(ShipCheckService.describe("두리두리", "2021-05-21 17:38:00")).isEqualTo("발주_두리두리_2021년05월21일오후17");
        assertThat(ShipCheckService.describe("(주)농협목우촌", "2021-07-01 09:05:00")).isEqualTo("발주_(주)농협목우촌_2021년07월01일오전09");
        assertThat(ShipCheckService.describe("정오", "2026-11-25 12:00:00")).isEqualTo("발주_정오_2026년11월25일오후12");
        assertThat(ShipCheckService.describe("가".repeat(200), "2026-11-25 12:00:00")).hasSize(100);
    }

    @Test
    void 확인_묶음의_상점마다_헤더_한_건을_같은_확인_시각으로_만든다() {
        when(mapper.selectBatchSummary(any())).thenReturn(List.of(
                Map.of("shopCode", "S1", "shopName", "두리두리", "orderCount", 3L),
                Map.of("shopCode", "S2", "orderCount", 1)));   // 상점명이 없으면 코드로
        when(mapper.insert(any())).thenReturn(1);

        int created = service.recordBatch("2026-11-25 14:30:00", "admin", "관리자");

        assertThat(created).isEqualTo(2);
        ArgumentCaptor<ShipCheck> cap = ArgumentCaptor.forClass(ShipCheck.class);
        verify(mapper, times(2)).insert(cap.capture());
        ShipCheck first = cap.getAllValues().get(0);
        assertThat(first.getShopCode()).isEqualTo("S1");
        assertThat(first.getShipCheckDesc()).isEqualTo("발주_두리두리_2026년11월25일오후14");
        assertThat(first.getShipCheckDate()).isEqualTo("2026-11-25 14:30:00");
        assertThat(first.getOrderCount()).isEqualTo(3);
        assertThat(first.getPrintCount()).isZero();
        assertThat(first.getRegistId()).isEqualTo("admin");
        assertThat(cap.getAllValues().get(1).getShipCheckDesc()).startsWith("발주_S2_");
    }

    @Test
    void 확인된_주문이_없으면_헤더를_만들지_않는다() {
        when(mapper.selectBatchSummary(any())).thenReturn(List.of());

        assertThat(service.recordBatch("2026-11-25 14:30:00", "admin", "관리자")).isZero();
        verify(mapper, never()).insert(any());
    }
}
