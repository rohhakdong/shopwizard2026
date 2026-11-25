package com.shopwizard.order.service;

import com.shopwizard.order.mapper.ProdCodeMatchMapper;
import com.shopwizard.order.model.ProdCodeMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** 상품매칭결과 — 상품명·옵션은 trim 하지 않고, 옵션이 비면 "" 로, 상품코드는 판매 상품에 있어야 한다. */
class ProdCodeMatchServiceTest {

    private final ProdCodeMatchMapper mapper = mock(ProdCodeMatchMapper.class);
    private final ProdCodeMatchService service = new ProdCodeMatchService(mapper);

    @BeforeEach
    void setUp() {
        when(mapper.select(any())).thenReturn(null);
        when(mapper.countProd(anyString())).thenReturn(1);
    }

    private static ProdCodeMatch in(String name, String option, String code) {
        ProdCodeMatch m = new ProdCodeMatch();
        m.setProdName(name);
        m.setProdOption(option);
        m.setProdCode(code);
        m.setRegistId("u");
        m.setRegistName("사용자");
        return m;
    }

    private void expectBad(ProdCodeMatch m, String messagePart) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(m));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains(messagePart), ex.getReason());
        verify(mapper, never()).insert(any());
    }

    @Test
    void 상품명과_옵션은_입력_그대로_저장한다_끝_공백도_그대로() {
        service.create(in("5분완성! 라면밥 1팩 두리두리 ", "치즈라면밥", "1000000296"));

        ArgumentCaptor<ProdCodeMatch> cap = ArgumentCaptor.forClass(ProdCodeMatch.class);
        verify(mapper).insert(cap.capture());
        assertEquals("5분완성! 라면밥 1팩 두리두리 ", cap.getValue().getProdName());   // 주문 줄 원문과 정확히 비교되므로
        assertEquals("치즈라면밥", cap.getValue().getProdOption());
        assertEquals("1000000296", cap.getValue().getProdCode());
        assertEquals("u", cap.getValue().getRegistId());
    }

    @Test
    void 옵션을_비우면_빈_문자열이다_null_이_아니다() {
        service.create(in("상품", null, "1000000296"));

        ArgumentCaptor<ProdCodeMatch> cap = ArgumentCaptor.forClass(ProdCodeMatch.class);
        verify(mapper).insert(cap.capture());
        assertEquals("", cap.getValue().getProdOption());
        verify(mapper, atLeastOnce()).select(Map.of("prodName", "상품", "prodOption", ""));
    }

    @Test
    void 상품코드는_앞뒤_공백만_잘라_판매_상품에서_확인한다() {
        service.create(in("상품", "", " 1000000296 "));

        verify(mapper).countProd("1000000296");
    }

    @Test
    void 필수값_길이_판매상품_존재를_검사한다() {
        expectBad(in(null, "", "1000000296"), "상품명");
        expectBad(in("   ", "", "1000000296"), "상품명");
        expectBad(in("상품", "", " "), "상품코드");
        expectBad(in("가".repeat(256), "", "1000000296"), "상품명");
        expectBad(in("상품", "가".repeat(256), "1000000296"), "옵션");
        when(mapper.countProd("없는코드")).thenReturn(0);
        expectBad(in("상품", "", "없는코드"), "판매 상품에 없는");
    }

    @Test
    void 같은_상품명_옵션_규칙이_이미_있으면_409() {
        when(mapper.select(Map.of("prodName", "상품", "prodOption", "옵션"))).thenReturn(new ProdCodeMatch());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(in("상품", "옵션", "1000000296")));

        assertEquals(409, ex.getStatusCode().value());
        verify(mapper, never()).insert(any());
    }

    @Test
    void 수정은_상품코드만_바꾼다() {
        ProdCodeMatch existing = in("상품", "옵션", "old");
        when(mapper.select(Map.of("prodName", "상품", "prodOption", "옵션"))).thenReturn(existing);
        ProdCodeMatch req = in("상품", "옵션", "1000000342");
        req.setChangeId("u2");
        req.setChangeName("수정자");

        service.modify(req);

        ArgumentCaptor<ProdCodeMatch> cap = ArgumentCaptor.forClass(ProdCodeMatch.class);
        verify(mapper).update(cap.capture());
        assertEquals("1000000342", cap.getValue().getProdCode());
        assertEquals("상품", cap.getValue().getProdName());
        assertEquals("u2", cap.getValue().getChangeId());
    }

    @Test
    void 없는_규칙을_수정_삭제하면_400_상품이_없으면_수정도_400() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.modify(in("없음", "", "1000000296"))).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.remove("없음", "")).getStatusCode().value());

        ProdCodeMatch existing = in("상품", "옵션", "old");
        when(mapper.select(Map.of("prodName", "상품", "prodOption", "옵션"))).thenReturn(existing);
        when(mapper.countProd("없는코드")).thenReturn(0);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.modify(in("상품", "옵션", "없는코드"))).getStatusCode().value());
        verify(mapper, never()).update(any());

        service.remove("상품", "옵션");
        verify(mapper).delete(existing);
    }
}
