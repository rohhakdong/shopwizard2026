package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderUpriceMapper;
import com.shopwizard.order.model.OrderUprice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 발주단가표 — 코드 생성·중복 규칙(코드·같은 상점의 같은 발주상품명칭)·수정 가능 필드·사용 중인 코드 삭제 차단. */
class OrderUpriceServiceTest {

    private final OrderUpriceMapper mapper = mock(OrderUpriceMapper.class);
    private final OrderUpriceService service = new OrderUpriceService(mapper);

    @BeforeEach
    void setUp() {
        when(mapper.select(any())).thenReturn(null);
        when(mapper.countSameName(any(), anyString(), anyInt(), anyString())).thenReturn(0);
        when(mapper.countUsage(anyString())).thenReturn(0);
    }

    private static OrderUprice in() {
        OrderUprice u = new OrderUprice();
        u.setShopCode("2068542226A1");
        u.setOrderProdName("H 32_ 황기 닭백숙 1kg");
        u.setKeywrd("황기 닭백숙");
        u.setCapa("1kg");
        u.setUprice(5370);
        u.setUnitName("개");
        u.setRegistId("u");
        u.setRegistName("사용자");
        return u;
    }

    private void expectBad(OrderUprice u, String messagePart) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(u));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains(messagePart), ex.getReason());
        verify(mapper, never()).insert(any());
    }

    @Test
    void 등록하면_코드는_키워드_하이픈_용량이고_사용여부_기본은_1이다() {
        service.create(in());

        ArgumentCaptor<OrderUprice> cap = ArgumentCaptor.forClass(OrderUprice.class);
        verify(mapper).insert(cap.capture());
        OrderUprice row = cap.getValue();
        assertEquals("황기 닭백숙-1kg", row.getMatchUpriceCode());
        assertEquals(5370, row.getUprice());
        assertEquals(1, row.getUseYn());
        assertEquals("성공", row.getResultType());
        assertEquals(1, row.getState());
        assertEquals("2068542226A1", row.getShopCode());
    }

    @Test
    void 상점을_비우면_공통_코드로_저장한다() {
        OrderUprice u = in();
        u.setShopCode("  ");

        service.create(u);

        ArgumentCaptor<OrderUprice> cap = ArgumentCaptor.forClass(OrderUprice.class);
        verify(mapper).insert(cap.capture());
        assertEquals("", cap.getValue().getShopCode());
        verify(mapper).countSameName(null, "H 32_ 황기 닭백숙 1kg", 1, "황기 닭백숙-1kg");
    }

    @Test
    void 코드가_이미_있거나_같은_상점의_같은_발주상품명칭이_사용_중이면_409() {
        when(mapper.select(Map.of("matchUpriceCode", "황기 닭백숙-1kg"))).thenReturn(new OrderUprice());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.create(in())).getStatusCode().value());

        when(mapper.select(any())).thenReturn(null);
        when(mapper.countSameName("2068542226A1", "H 32_ 황기 닭백숙 1kg", 1, "황기 닭백숙-1kg")).thenReturn(1);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(in()));
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("중복"));
        verify(mapper, never()).insert(any());
    }

    @Test
    void 필수값과_단가_범위를_검사한다() {
        OrderUprice u = in(); u.setOrderProdName(" "); expectBad(u, "발주상품명칭");
        u = in(); u.setKeywrd(null); expectBad(u, "키워드");
        u = in(); u.setCapa(""); expectBad(u, "용량");
        u = in(); u.setUprice(null); expectBad(u, "단가");
        u = in(); u.setUprice(-1); expectBad(u, "단가");
        u = in(); u.setUprice(100_000_000); expectBad(u, "단가");
        u = in(); u.setUseYn(5); expectBad(u, "사용여부");
        u = in(); u.setShopCode("1".repeat(13)); expectBad(u, "상점");
        u = in(); u.setUnitName("가".repeat(51)); expectBad(u, "단량");
        u = in(); u.setUprice(0); service.create(u);   // 0원 단가는 허용
        verify(mapper, times(1)).insert(any());
    }

    @Test
    void 수정은_상점_단가_단량_사용여부만_바꾼다() {
        OrderUprice existing = in();
        existing.setMatchUpriceCode("황기 닭백숙-1kg");
        existing.setUseYn(1);
        when(mapper.select(Map.of("matchUpriceCode", "황기 닭백숙-1kg"))).thenReturn(existing);
        OrderUprice req = new OrderUprice();
        req.setMatchUpriceCode("황기 닭백숙-1kg");
        req.setShopCode("3018508386A1");
        req.setUprice(6000);
        req.setUnitName("팩");
        req.setUseYn(0);
        req.setOrderProdName("바꾸려는 이름");   // 무시
        req.setKeywrd("다른 키워드");              // 무시
        req.setChangeId("u2");

        service.modify(req);

        ArgumentCaptor<OrderUprice> cap = ArgumentCaptor.forClass(OrderUprice.class);
        verify(mapper).update(cap.capture());
        OrderUprice row = cap.getValue();
        assertEquals("3018508386A1", row.getShopCode());
        assertEquals(6000, row.getUprice());
        assertEquals("팩", row.getUnitName());
        assertEquals(0, row.getUseYn());
        assertEquals("H 32_ 황기 닭백숙 1kg", row.getOrderProdName());
        assertEquals("황기 닭백숙", row.getKeywrd());
        assertEquals("수정", row.getResultType());
        assertEquals("u2", row.getChangeId());
        verify(mapper).countSameName("3018508386A1", "H 32_ 황기 닭백숙 1kg", 0, "황기 닭백숙-1kg");   // 자기 코드는 중복 검사에서 제외
    }

    @Test
    void 수정으로_같은_발주상품명칭이_겹치면_409_없는_코드면_400() {
        OrderUprice existing = in();
        existing.setMatchUpriceCode("황기 닭백숙-1kg");
        when(mapper.select(Map.of("matchUpriceCode", "황기 닭백숙-1kg"))).thenReturn(existing);
        when(mapper.countSameName(any(), anyString(), anyInt(), eq("황기 닭백숙-1kg"))).thenReturn(1);
        OrderUprice req = new OrderUprice();
        req.setMatchUpriceCode("황기 닭백숙-1kg");
        req.setUprice(1);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.modify(req)).getStatusCode().value());

        OrderUprice missing = new OrderUprice();
        missing.setMatchUpriceCode("없는코드");
        missing.setUprice(1);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.modify(missing)).getStatusCode().value());
        verify(mapper, never()).update(any());
    }

    @Test
    void 단가매칭이나_발주견적이_쓰는_코드는_지울_수_없다() {
        OrderUprice existing = in();
        existing.setMatchUpriceCode("쓰는코드");
        when(mapper.select(Map.of("matchUpriceCode", "쓰는코드"))).thenReturn(existing);
        when(mapper.countUsage("쓰는코드")).thenReturn(3);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.remove("쓰는코드"));

        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("3건"));
        verify(mapper, never()).delete(any());
    }

    @Test
    void 안_쓰는_코드는_지우고_없는_코드는_400() {
        OrderUprice existing = in();
        existing.setMatchUpriceCode("안쓰는코드");
        when(mapper.select(Map.of("matchUpriceCode", "안쓰는코드"))).thenReturn(existing);

        service.remove("안쓰는코드");
        verify(mapper).delete(existing);

        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.remove("없는코드")).getStatusCode().value());
    }
}
