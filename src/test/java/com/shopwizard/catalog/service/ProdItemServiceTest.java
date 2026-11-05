package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdItemMapper;
import com.shopwizard.catalog.mapper.ProdMapper;
import com.shopwizard.catalog.model.ProdItem;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 옵션 변경이 게시된 상품의 판매 옵션(shopion.tPrdProdItem)에 반영되는지. 재고 수량은 upsert 쪽 SQL 이 제외한다. */
class ProdItemServiceTest {

    private final ProdItemMapper itemMapper = mock(ProdItemMapper.class);
    private final ProdMapper prodMapper = mock(ProdMapper.class);
    private final ProdItemService service = new ProdItemService(itemMapper, prodMapper);

    private static ProdItem item(String code, int itemCode) {
        ProdItem i = new ProdItem();
        i.setProdCode(code);
        i.setItemCode(itemCode);
        return i;
    }

    private void assertSynced(String code) {
        InOrder o = inOrder(itemMapper);
        o.verify(itemMapper).deleteMissing2Shopion(code);
        o.verify(itemMapper).upsert2Shopion(code);
    }

    @Test
    void 게시된_상품에_옵션을_추가_수정_삭제하면_판매_옵션을_동기화한다() {
        when(prodMapper.countShopion("P1")).thenReturn(1);

        service.insert(item("P1", 20001));
        verify(itemMapper).insert(any());
        assertSynced("P1");

        clearInvocations(itemMapper);
        service.update(item("P1", 20001));
        verify(itemMapper).update(any());
        assertSynced("P1");

        clearInvocations(itemMapper);
        service.delete(item("P1", 20001));
        verify(itemMapper).delete(any());
        assertSynced("P1");
    }

    @Test
    void 게시되지_않은_상품의_옵션은_판매_쪽을_건드리지_않는다() {
        when(prodMapper.countShopion("P1")).thenReturn(0);

        service.insert(item("P1", 20001));
        service.update(item("P1", 20001));
        service.delete(item("P1", 20001));

        verify(itemMapper, never()).deleteMissing2Shopion(any());
        verify(itemMapper, never()).upsert2Shopion(any());
    }

    @Test
    void 승인_게시는_상품이_이미_복사됐으면_옵션을_동기화한다() {
        // 상품 본체 copy2shopion 이 먼저 실행되므로 이 시점엔 판매 상품이 있다 — upsert 가 새 옵션을 넣는다.
        Map<String, Object> params = new HashMap<>();
        params.put("prodCode", "P1");
        when(prodMapper.countShopion("P1")).thenReturn(1);

        service.copy2Shopion(params);

        assertSynced("P1");
        verify(itemMapper, never()).copy2Shopion(any());
    }

    @Test
    void 판매_상품이_없으면_예전처럼_단순_복사한다() {
        Map<String, Object> params = new HashMap<>();
        params.put("prodCode", "P1");
        when(prodMapper.countShopion("P1")).thenReturn(0);
        when(itemMapper.copy2Shopion(params)).thenReturn(2);

        assertEquals(2, service.copy2Shopion(params));

        verify(itemMapper, never()).upsert2Shopion(any());
    }

    @Test
    void 옵션목록_수정_사용불가_처리도_게시된_상품이면_동기화한다() {
        when(prodMapper.countShopion("P1")).thenReturn(1);
        Map<String, Object> params = new HashMap<>();
        params.put("prodCode", "P1");

        service.updateItemlist(params);
        assertSynced("P1");

        clearInvocations(itemMapper);
        service.disable("P1");
        assertSynced("P1");

        clearInvocations(itemMapper);
        service.enable(params);
        assertSynced("P1");
    }
}
