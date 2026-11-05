package com.shopwizard.product.service;

import com.shopwizard.framework.util.DeleteResult;
import com.shopwizard.product.mapper.ProdMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 판매 상품 관리의 삭제 — 주문 이력이 있으면 지우지 않고 완전품절로만 바꾼다. */
class ProdServiceTest {

    private final ProdMapper prodMapper = mock(ProdMapper.class);
    private final ProdService service = new ProdService(prodMapper);

    @Test
    @SuppressWarnings("unchecked")
    void 주문_이력이_있으면_삭제하지_않고_판매_등록_양쪽을_완전품절로_바꾼다() {
        when(prodMapper.countOrderLines("P1")).thenReturn(2);

        DeleteResult r = service.delete("P1");

        assertFalse(r.deleted());
        assertTrue(r.message().contains("완전품절"));
        verify(prodMapper, never()).delete(any());
        ArgumentCaptor<Map<String, Object>> live = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> cat = ArgumentCaptor.forClass(Map.class);
        verify(prodMapper).updateSaleYn(live.capture());
        verify(prodMapper).updateCatSaleYn(cat.capture());
        assertEquals("P1", live.getValue().get("prodCode"));
        assertEquals(-9, live.getValue().get("saleYn"));
        assertEquals("P1", cat.getValue().get("prodCode"));
        assertEquals(-9, cat.getValue().get("saleYn"));
    }

    @Test
    void 주문_이력이_없으면_실제로_삭제한다() {
        when(prodMapper.countOrderLines("P1")).thenReturn(0);

        DeleteResult r = service.delete("P1");

        assertTrue(r.deleted());
        assertNull(r.message());
        verify(prodMapper).delete("P1");
        verify(prodMapper, never()).updateSaleYn(any());
        verify(prodMapper, never()).updateCatSaleYn(any());
    }
}
