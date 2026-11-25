package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderUpriceMapper;
import com.shopwizard.order.mapper.UpriceMatchMapper;
import com.shopwizard.order.model.OrderUprice;
import com.shopwizard.order.model.UpriceMatchDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 단가매칭 규칙 — 줄을 모두 검증한 뒤에만 쓰고(검증 실패 시 기존 규칙은 그대로), 규칙 목록·삭제. */
class UpriceMatchServiceTest {

    private final UpriceMatchMapper matchMapper = mock(UpriceMatchMapper.class);
    private final OrderUpriceMapper upriceMapper = mock(OrderUpriceMapper.class);
    private final UpriceMatchService service = new UpriceMatchService(matchMapper, upriceMapper);

    @BeforeEach
    void setUp() {
        when(upriceMapper.select(any())).thenReturn(new OrderUprice());   // 모든 코드가 발주단가표에 있다고 본다
    }

    private static UpriceMatchDetail line(String code, Integer qty) {
        UpriceMatchDetail d = new UpriceMatchDetail();
        d.setMatchUpriceCode(code);
        d.setQty(qty);
        return d;
    }

    private void expectBad(String prodName, String itemName, List<UpriceMatchDetail> lines, String messagePart) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.saveMatch(prodName, itemName, lines, "u", "사용자"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains(messagePart), ex.getReason());
        verify(matchMapper, never()).deleteByProdItem(any(), any());   // 검증이 먼저 — 실패하면 기존 규칙은 지워지지 않는다
        verify(matchMapper, never()).insertDetail(any());
    }

    @Test
    void 저장은_기존_줄을_지우고_순번을_1부터_다시_넣는다_이름은_trim_하지_않는다() {
        service.saveMatch("상품명 ", null, List.of(line("A-1kg", 2), line("B-500g", 1)), "u", "사용자");

        verify(matchMapper).deleteByProdItem("상품명 ", "");
        ArgumentCaptor<UpriceMatchDetail> cap = ArgumentCaptor.forClass(UpriceMatchDetail.class);
        verify(matchMapper, times(2)).insertDetail(cap.capture());
        assertEquals(List.of(1, 2), cap.getAllValues().stream().map(UpriceMatchDetail::getMatchSeq).toList());
        assertEquals(List.of("A-1kg", "B-500g"), cap.getAllValues().stream().map(UpriceMatchDetail::getMatchUpriceCode).toList());
        assertEquals("상품명 ", cap.getValue().getProdName());
        assertEquals("", cap.getValue().getItemName());
        assertEquals("u", cap.getValue().getRegistId());
    }

    @Test
    void 상품명_줄_수량_코드를_검증한다() {
        expectBad(null, "", List.of(line("A-1kg", 1)), "상품명");
        expectBad("  ", "", List.of(line("A-1kg", 1)), "상품명");
        expectBad("상품", "", null, "1줄 이상");
        expectBad("상품", "", List.of(), "1줄 이상");
        expectBad("상품", "", List.of(line(" ", 1)), "비어 있는");
        expectBad("상품", "", List.of(line("A-1kg", null)), "수량");
        expectBad("상품", "", List.of(line("A-1kg", 0)), "수량");
        expectBad("상품", "", List.of(line("A-1kg", 10_000)), "수량");
        expectBad("가".repeat(256), "", List.of(line("A-1kg", 1)), "상품명");
        expectBad("상품", "가".repeat(256), List.of(line("A-1kg", 1)), "옵션");
    }

    @Test
    void 같은_코드가_두_줄이거나_발주단가표에_없는_코드면_거절한다() {
        expectBad("상품", "", List.of(line("A-1kg", 1), line("A-1kg", 2)), "두 줄 이상");

        when(upriceMapper.select(Map.of("matchUpriceCode", "없는코드"))).thenReturn(null);
        expectBad("상품", "", List.of(line("A-1kg", 1), line("없는코드", 1)), "발주단가표에 없는");
    }

    @Test
    void 규칙은_30줄까지() {
        List<UpriceMatchDetail> many = new java.util.ArrayList<>();
        for (int i = 0; i < 31; i++) many.add(line("C" + i, 1));
        expectBad("상품", "", many, "30줄");
    }

    @Test
    void 규칙_삭제는_있는_규칙만_전체를_지운다() {
        when(matchMapper.selectRuleLines("상품", "")).thenReturn(List.of());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.deleteRule("상품", null)).getStatusCode().value());
        verify(matchMapper, never()).deleteByProdItem(any(), any());

        when(matchMapper.selectRuleLines("상품", "옵션")).thenReturn(List.of(line("A-1kg", 1), line("B-500g", 1)));
        service.deleteRule("상품", "옵션");
        verify(matchMapper).deleteByProdItem("상품", "옵션");
    }

    @Test
    void 규칙_목록과_줄_조회는_매퍼에_위임하고_옵션이_null_이면_빈_옵션이다() {
        Map<String, Object> params = Map.of("pProdName", "x");
        when(matchMapper.selectRules(params)).thenReturn(List.of(Map.of("prodName", "x")));
        when(matchMapper.selectRuleCount(params)).thenReturn(7);

        assertEquals(1, service.selectRules(params).size());
        assertEquals(7, service.selectRuleCount(params));
        service.selectRuleLines("상품", null);
        verify(matchMapper).selectRuleLines("상품", "");
    }
}
