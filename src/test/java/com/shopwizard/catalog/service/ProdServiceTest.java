package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdChnlMapper;
import com.shopwizard.catalog.mapper.ProdImgMapper;
import com.shopwizard.catalog.mapper.ProdItemMapper;
import com.shopwizard.catalog.mapper.ProdMapper;
import com.shopwizard.catalog.model.Prod;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 승인 후 수정·삭제가 판매 상품(shopion.tPrdProd)에 반영되는지 — 등록 쪽(tCatProd)이 원본. */
class ProdServiceTest {

    private final ProdMapper prodMapper = mock(ProdMapper.class);
    private final ProdItemMapper prodItemMapper = mock(ProdItemMapper.class);
    private final ProdChnlMapper prodChnlMapper = mock(ProdChnlMapper.class);
    private final ProdImgMapper prodImgMapper = mock(ProdImgMapper.class);
    private final ProdService service = new ProdService(prodMapper, prodItemMapper, prodChnlMapper, prodImgMapper);

    private static Prod prod(String code) {
        Prod p = new Prod();
        p.setProdCode(code);
        return p;
    }

    // ── 수정 ────────────────────────────────────────────────

    @Test
    void 게시된_상품을_수정하면_판매_상품에도_반영한다() {
        when(prodMapper.update(any())).thenReturn(1);
        when(prodMapper.countShopion("P1")).thenReturn(1);

        assertEquals(1, service.update(prod("P1")));

        InOrder o = inOrder(prodMapper);
        o.verify(prodMapper).update(any());
        o.verify(prodMapper).sync2Shopion("P1");
    }

    @Test
    void 아직_게시되지_않은_상품은_판매_쪽을_건드리지_않는다() {
        when(prodMapper.update(any())).thenReturn(1);
        when(prodMapper.countShopion("P1")).thenReturn(0);

        service.update(prod("P1"));

        verify(prodMapper, never()).sync2Shopion(any());
    }

    @Test
    void 수정_요청에_상품코드가_없으면_상점상품코드로_찾아_반영한다() {
        Prod p = new Prod();
        p.setShopProdCode("SP-9");
        when(prodMapper.selectProdCode("SP-9")).thenReturn("P9");
        when(prodMapper.countShopion("P9")).thenReturn(1);

        service.update(p);

        verify(prodMapper).sync2Shopion("P9");
    }

    @Test
    void 판매_쪽_반영이_실패하면_예외가_그대로_올라간다_트랜잭션_롤백용() {
        when(prodMapper.countShopion("P1")).thenReturn(1);
        when(prodMapper.sync2Shopion("P1")).thenThrow(new IllegalStateException("db"));

        assertThrows(IllegalStateException.class, () -> service.update(prod("P1")));
    }

    // ── 삭제 ────────────────────────────────────────────────

    @Test
    void 주문_이력이_있으면_409로_막고_아무것도_지우지_않는다() {
        when(prodMapper.countOrderLines("P1")).thenReturn(3);
        when(prodMapper.countShopion("P1")).thenReturn(1);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.delete(prod("P1")));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertTrue(e.getReason().contains("주문 이력"));
        verify(prodMapper, never()).delete(any());
        verify(prodMapper, never()).delete2Shopion(any());
        verify(prodItemMapper, never()).deleteByProdCode(any());
        verify(prodItemMapper, never()).delete2Shopion(any());
    }

    @Test
    void 게시된_상품을_삭제하면_판매_쪽_자식_본체_순으로_지운_뒤_등록_쪽을_지운다() {
        when(prodMapper.countOrderLines("P1")).thenReturn(0);
        when(prodMapper.countShopion("P1")).thenReturn(1);
        when(prodMapper.delete(any())).thenReturn(1);

        assertEquals(1, service.delete(prod("P1")));

        InOrder o = inOrder(prodItemMapper, prodChnlMapper, prodImgMapper, prodMapper);
        o.verify(prodItemMapper).delete2Shopion("P1");
        o.verify(prodChnlMapper).delete2Shopion("P1");
        o.verify(prodImgMapper).delete2Shopion("P1");
        o.verify(prodMapper).delete2ShopionStoreProd("P1");
        o.verify(prodMapper).delete2ShopionLayoutProd("P1");
        o.verify(prodMapper).delete2ShopionEventProd("P1");
        o.verify(prodMapper).delete2Shopion("P1");
        o.verify(prodItemMapper).deleteByProdCode("P1");
        o.verify(prodMapper).delete(any());
    }

    @Test
    void 게시되지_않은_상품은_등록_쪽만_지운다() {
        when(prodMapper.countOrderLines("P1")).thenReturn(0);
        when(prodMapper.countShopion("P1")).thenReturn(0);

        service.delete(prod("P1"));

        verify(prodMapper, never()).delete2Shopion(any());
        verify(prodItemMapper, never()).delete2Shopion(any());
        verify(prodItemMapper).deleteByProdCode("P1");
        verify(prodMapper).delete(any());
    }

    // ── 승인 게시 ───────────────────────────────────────────

    @Test
    void 승인_게시는_판매_쪽에_없으면_복사한다() {
        Map<String, Object> params = new HashMap<>();
        params.put("prodCode", "P1");
        when(prodMapper.countShopion("P1")).thenReturn(0);
        when(prodMapper.copy2Shopion(params)).thenReturn(1);

        assertEquals(1, service.copy2Shopion(params));

        verify(prodMapper).copy2Shopion(params);
        verify(prodMapper, never()).sync2Shopion(any());
    }

    @Test
    void 재승인은_중복_오류_대신_동기화한다() {
        Map<String, Object> params = new HashMap<>();
        params.put("prodCode", "P1");
        when(prodMapper.countShopion("P1")).thenReturn(1);
        when(prodMapper.sync2Shopion("P1")).thenReturn(1);

        assertEquals(1, service.copy2Shopion(params));

        verify(prodMapper).sync2Shopion("P1");
        verify(prodMapper, never()).copy2Shopion(any());
    }
}
