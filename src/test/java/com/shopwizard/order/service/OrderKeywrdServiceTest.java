package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderKeywrdMapper;
import com.shopwizard.order.mapper.KeywordTargetMapper;
import com.shopwizard.order.model.OrderKeywrd;
import com.shopwizard.order.model.OrderProd;
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

/** 키워드 규칙 — 검증(구분·중복·상점)과 값 정리, 수정에서 사용 여부 유지, 배정 writer 의 가드. */
class OrderKeywrdServiceTest {

    private final OrderKeywrdMapper mapper = mock(OrderKeywrdMapper.class);
    private final OrderKeywrdService service = new OrderKeywrdService(mapper);

    @BeforeEach
    void setUp() {
        when(mapper.countShop(anyString())).thenReturn(1);
        when(mapper.countDuplicate(anyString(), anyString(), anyString(), any())).thenReturn(0);
        when(mapper.select(any())).thenReturn(null);
        doAnswer(inv -> { ((OrderKeywrd) inv.getArgument(0)).setKeywrdSeq(500); return 1; }).when(mapper).insert(any());
    }

    private static OrderKeywrd in() {
        OrderKeywrd k = new OrderKeywrd();
        k.setShopCode(" 2148817433A1 ");
        k.setKeywrdSect(" like  option ");
        k.setKeywrdName("  매트 ");
        k.setRegistId("u");
        k.setRegistName("사용자");
        return k;
    }

    private void expectBad(OrderKeywrd k, String part) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(k));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains(part), ex.getReason());
        verify(mapper, never()).insert(any());
    }

    @Test
    void 등록하면_공백과_구분_표기를_정리해_저장하고_사용_중으로_시작한다() {
        service.create(in());

        ArgumentCaptor<OrderKeywrd> cap = ArgumentCaptor.forClass(OrderKeywrd.class);
        verify(mapper).insert(cap.capture());
        assertEquals("2148817433A1", cap.getValue().getShopCode());
        assertEquals("LIKE OPTION", cap.getValue().getKeywrdSect());
        assertEquals("매트", cap.getValue().getKeywrdName());
        assertEquals(1, cap.getValue().getState());
        assertEquals("u", cap.getValue().getRegistId());
    }

    @Test
    void 필수값_구분_길이_중복_상점을_검사한다() {
        OrderKeywrd k = in(); k.setShopCode(" "); expectBad(k, "상점");
        k = in(); k.setKeywrdName(" "); expectBad(k, "키워드");
        k = in(); k.setKeywrdName("가".repeat(51)); expectBad(k, "50자");
        k = in(); k.setKeywrdSect("EQUALS"); expectBad(k, "구분");
        k = in(); k.setKeywrdSect(null); expectBad(k, "구분");
        k = in(); k.setRemark("가".repeat(51)); expectBad(k, "비고");
        k = in(); k.setState(2); expectBad(k, "사용 여부");

        when(mapper.countDuplicate(anyString(), eq("LIKE OPTION"), eq("매트"), any())).thenReturn(1);
        expectBad(in(), "이미 있는");

        when(mapper.countShop("NOPE")).thenReturn(0);
        k = in(); k.setShopCode("NOPE"); expectBad(k, "없는 상점");
    }

    @Test
    void 수정은_자기_자신을_중복_검사에서_빼고_사용_여부를_비우면_지금_값을_유지한다() {
        OrderKeywrd e = new OrderKeywrd();
        e.setKeywrdSeq(7);
        e.setState(0);
        when(mapper.select(Map.of("keywrdSeq", 7))).thenReturn(e);
        OrderKeywrd req = in(); req.setKeywrdSeq(7);

        service.modify(req);

        ArgumentCaptor<OrderKeywrd> cap = ArgumentCaptor.forClass(OrderKeywrd.class);
        verify(mapper).update(cap.capture());
        assertEquals(7, cap.getValue().getKeywrdSeq());
        assertEquals(0, cap.getValue().getState());
        verify(mapper).countDuplicate("2148817433A1", "LIKE OPTION", "매트", 7);
    }

    @Test
    void 없는_키워드를_수정_삭제하면_400이고_삭제는_그_행만_지운다() {
        OrderKeywrd req = in(); req.setKeywrdSeq(99);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.modify(req)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.remove(99)).getStatusCode().value());

        OrderKeywrd e = new OrderKeywrd(); e.setKeywrdSeq(3);
        when(mapper.select(Map.of("keywrdSeq", 3))).thenReturn(e);
        service.remove(3);
        verify(mapper).delete(e);
    }

    // ── 배정 writer ─────────────────────────────────────────────

    private final OrderLineLock lineLock = mock(OrderLineLock.class);
    private final KeywordTargetMapper targetMapper = mock(KeywordTargetMapper.class);
    private final KeywordAssignWriter writer = new KeywordAssignWriter(lineLock, targetMapper);

    private static OrderProd line(String state, String shop) {
        OrderProd op = new OrderProd();
        op.setOrderState(state);
        op.setShopCode(shop);
        return op;
    }

    @Test
    void 배정은_지불완료이고_상점이_비어_있을_때만_쓴다() {
        when(lineLock.lockLine(1, 1)).thenReturn(line("지불완료", null));
        when(lineLock.lockLine(2, 1)).thenReturn(line("지불완료", "  "));
        when(lineLock.lockLine(3, 1)).thenReturn(line("지불완료", "OTHER"));      // 먼저 다른 상점이 가져감
        when(lineLock.lockLine(4, 1)).thenReturn(line("출고지시", null));         // 그 사이 출고지시
        when(lineLock.lockLine(5, 1)).thenReturn(null);
        when(targetMapper.assignShop(anyInt(), anyInt(), anyString(), anyString(), any(), any())).thenReturn(1);

        assertTrue(writer.assign(1, 1, "S1", "상점1", "u", "n"));
        assertTrue(writer.assign(2, 1, "S1", "상점1", "u", "n"));
        assertFalse(writer.assign(3, 1, "S1", "상점1", "u", "n"));
        assertFalse(writer.assign(4, 1, "S1", "상점1", "u", "n"));
        assertFalse(writer.assign(5, 1, "S1", "상점1", "u", "n"));
        verify(targetMapper, times(2)).assignShop(anyInt(), anyInt(), eq("S1"), eq("상점1"), eq("u"), eq("n"));
    }

    @Test
    void 좁은_UPDATE_가_0행이면_배정_실패다() {
        when(lineLock.lockLine(1, 1)).thenReturn(line("지불완료", null));
        when(targetMapper.assignShop(anyInt(), anyInt(), anyString(), anyString(), any(), any())).thenReturn(0);

        assertFalse(writer.assign(1, 1, "S1", "상점1", "u", "n"));
    }
}
