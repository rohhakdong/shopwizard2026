package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdItemMapper;
import com.shopwizard.catalog.mapper.ProdMapper;
import com.shopwizard.catalog.model.ProdItem;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.HashMap;
import java.util.List;
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

    // ── 속성값에서 만든 옵션 반영 (엑셀 일괄등록) ─────────────

    private static ProdItem existingItem(int code, String attrVal1, int saleYn) {
        ProdItem i = new ProdItem();
        i.setProdCode("P1");
        i.setItemCode(code);
        i.setAttrVal1(attrVal1); i.setAttrVal2(""); i.setAttrVal3(""); i.setAttrVal4("");
        i.setSaleYn(saleYn);
        return i;
    }

    private static com.shopwizard.catalog.model.Prod prod() {
        com.shopwizard.catalog.model.Prod p = new com.shopwizard.catalog.model.Prod();
        p.setProdCode("P1");
        p.setChangeId("admin1"); p.setChangeName("관리자");
        p.setLeadTime(5);
        return p;
    }

    private static String[] combo(String v1) { return new String[]{v1, "", "", ""}; }

    @Test
    void 속성값_옵션은_같은_조합은_다시_켜고_새_조합은_추가하고_목록에_없는_기존_옵션은_판매중지한다() {
        when(itemMapper.selectList(any())).thenReturn(List.of(
                existingItem(20000, "선택사항없음", 1),   // 기본 행 — 건드리지 않는다
                existingItem(20001, "S", 1),              // 그대로
                existingItem(20002, "M", 0),              // 판매중지였던 것 → 다시 켠다
                existingItem(20003, "XL", 1)));           // 새 목록에 없다 → 판매중지
        when(itemMapper.selectMaxItemCode("P1")).thenReturn(20003);
        when(prodMapper.countShopion("P1")).thenReturn(0);

        int created = service.applyAttributeOptions(prod(), List.of(combo("S"), combo("M"), combo("L")));

        assertEquals(1, created);
        @SuppressWarnings("unchecked") org.mockito.ArgumentCaptor<Map<String, Object>> en = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(itemMapper, times(1)).enable(en.capture());
        assertEquals(20002, en.getValue().get("itemCode"));

        org.mockito.ArgumentCaptor<ProdItem> ins = org.mockito.ArgumentCaptor.forClass(ProdItem.class);
        verify(itemMapper, times(1)).insert(ins.capture());
        ProdItem n = ins.getValue();
        assertEquals(20004, n.getItemCode());            // 기존 최대 다음 번호
        assertEquals("L", n.getAttrVal1());
        assertEquals("", n.getAttrVal2());
        assertEquals(9_999_999, n.getSupplyQty());       // 레거시 초기 재고
        assertEquals(1, n.getSaleYn());
        assertEquals(5, n.getLeadTime());                // 상품의 리드타임
        assertEquals("admin1", n.getRegistId());

        @SuppressWarnings("unchecked") org.mockito.ArgumentCaptor<Map<String, Object>> dis = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(itemMapper, times(1)).disableItem(dis.capture());
        assertEquals(20003, dis.getValue().get("itemCode"));   // XL 만 중지, 기본 행(20000)은 그대로
    }

    @Test
    void 옵션이_기본_행뿐인_상품은_20001부터_번호를_받는다() {
        when(itemMapper.selectList(any())).thenReturn(List.of(existingItem(20000, "선택사항없음", 1)));
        when(itemMapper.selectMaxItemCode("P1")).thenReturn(20000);

        service.applyAttributeOptions(prod(), List.of(combo("S"), combo("M"), combo("L")));

        org.mockito.ArgumentCaptor<ProdItem> ins = org.mockito.ArgumentCaptor.forClass(ProdItem.class);
        verify(itemMapper, times(3)).insert(ins.capture());
        assertEquals(List.of(20001, 20002, 20003), ins.getAllValues().stream().map(ProdItem::getItemCode).toList());
        verify(itemMapper, never()).disableItem(any());   // 기본 행은 중지하지 않는다
    }

    @Test
    void 옵션이_하나도_없는_상품도_20001부터() {
        when(itemMapper.selectList(any())).thenReturn(List.of());
        when(itemMapper.selectMaxItemCode("P1")).thenReturn(null);

        service.applyAttributeOptions(prod(), List.<String[]>of(combo("S")));

        org.mockito.ArgumentCaptor<ProdItem> ins = org.mockito.ArgumentCaptor.forClass(ProdItem.class);
        verify(itemMapper).insert(ins.capture());
        assertEquals(20001, ins.getValue().getItemCode());
    }

    @Test
    void 이미_판매중지인_옵션은_다시_중지하지_않고_여러_속성값_조합도_정확히_찾는다() {
        ProdItem sRed = existingItem(20001, "S", 1); sRed.setAttrVal2("빨강");
        ProdItem sBlue = existingItem(20002, "S", 0); sBlue.setAttrVal2("파랑");
        when(itemMapper.selectList(any())).thenReturn(List.of(existingItem(20000, "선택사항없음", 1), sRed, sBlue));
        when(itemMapper.selectMaxItemCode("P1")).thenReturn(20002);

        // S/빨강 은 그대로, S/파랑 은 목록에 없지만 이미 0 이라 다시 건드리지 않는다
        int created = service.applyAttributeOptions(prod(), List.<String[]>of(new String[]{"S", "빨강", "", ""}));

        assertEquals(0, created);
        verify(itemMapper, never()).insert(any());
        verify(itemMapper, never()).enable(any());
        verify(itemMapper, never()).disableItem(any());
    }

    @Test
    void 게시된_상품이면_옵션을_반영한_뒤_판매_옵션을_한_번만_동기화한다() {
        when(itemMapper.selectList(any())).thenReturn(List.of(existingItem(20000, "선택사항없음", 1)));
        when(itemMapper.selectMaxItemCode("P1")).thenReturn(20000);
        when(prodMapper.countShopion("P1")).thenReturn(1);

        service.applyAttributeOptions(prod(), List.of(combo("S"), combo("M")));

        verify(itemMapper, times(2)).insert(any());
        verify(itemMapper, times(1)).deleteMissing2Shopion("P1");   // insert 마다가 아니라 끝에 한 번
        verify(itemMapper, times(1)).upsert2Shopion("P1");
    }

    @Test
    void 게시되지_않은_상품은_판매_쪽을_건드리지_않는다() {
        when(itemMapper.selectList(any())).thenReturn(List.of(existingItem(20000, "선택사항없음", 1)));
        when(itemMapper.selectMaxItemCode("P1")).thenReturn(20000);
        when(prodMapper.countShopion("P1")).thenReturn(0);

        service.applyAttributeOptions(prod(), List.<String[]>of(combo("S")));

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
