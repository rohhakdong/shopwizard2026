package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderProdCodeMapper;
import com.shopwizard.order.model.OrderProdCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 발주상품코드 — 매칭상품코드 생성 규칙·검증·수정 가능 필드. */
class OrderProdCodeServiceTest {

    private final OrderProdCodeMapper mapper = mock(OrderProdCodeMapper.class);
    private final OrderProdCodeService service = new OrderProdCodeService(mapper);

    @BeforeEach
    void setUp() {
        when(mapper.select(any())).thenReturn(null);
    }

    private static OrderProdCode in() {
        OrderProdCode c = new OrderProdCode();
        c.setShopCode("2068542226A1");
        c.setProdCode("moguchon_0004");
        c.setProdName("목우촌 생생 황기닭백숙 1kg x 3개");
        c.setKeywrd("황기 닭백숙 삼계탕");
        c.setCapa1("1kg");
        c.setQty1("3");
        c.setRegistId("u");
        c.setRegistName("사용자");
        return c;
    }

    private void expectBad(OrderProdCode c, String messagePart) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(c));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains(messagePart), ex.getReason());
        verify(mapper, never()).insert(any());
    }

    @Test
    void 매칭상품코드는_키워드_콜론콜론_용량x수량으로_만든다() {
        assertEquals("황기 닭백숙 삼계탕::1kgx3", OrderProdCodeService.matchProdCode("황기 닭백숙 삼계탕", "1kg", "3", null, null));
        assertEquals("건전지::AAx20+AAAx20", OrderProdCodeService.matchProdCode("건전지", "AA", "20", "AAA", "20"));   // 실제 데이터 모양
    }

    @Test
    void 등록하면_코드를_서버가_만들고_사용여부는_기본_1_결과는_성공이다() {
        service.create(in());

        ArgumentCaptor<OrderProdCode> cap = ArgumentCaptor.forClass(OrderProdCode.class);
        verify(mapper).insert(cap.capture());
        OrderProdCode row = cap.getValue();
        assertEquals("황기 닭백숙 삼계탕::1kgx3", row.getMatchProdCode());
        assertEquals(1, row.getUseYn());
        assertEquals("성공", row.getResultType());
        assertEquals(1, row.getState());
        assertEquals("", row.getCapa2());
        assertEquals("", row.getQty2());
        assertEquals("u", row.getRegistId());
    }

    @Test
    void 용량2가_있으면_코드에_이어_붙는다() {
        OrderProdCode c = in();
        c.setCapa2("2kg");
        c.setQty2("1");

        service.create(c);

        ArgumentCaptor<OrderProdCode> cap = ArgumentCaptor.forClass(OrderProdCode.class);
        verify(mapper).insert(cap.capture());
        assertEquals("황기 닭백숙 삼계탕::1kgx3+2kgx1", cap.getValue().getMatchProdCode());
    }

    @Test
    void 이미_있는_코드면_409() {
        when(mapper.select(Map.of("matchProdCode", "황기 닭백숙 삼계탕::1kgx3"))).thenReturn(new OrderProdCode());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(in()));

        assertEquals(409, ex.getStatusCode().value());
        verify(mapper, never()).insert(any());
    }

    @Test
    void 필수값과_형식을_검사한다() {
        OrderProdCode c = in(); c.setShopCode(" "); expectBad(c, "상점");
        c = in(); c.setProdCode(null); expectBad(c, "상품코드");
        c = in(); c.setProdName(""); expectBad(c, "상품명칭");
        c = in(); c.setKeywrd(null); expectBad(c, "키워드");
        c = in(); c.setCapa1(""); expectBad(c, "용량1");
        c = in(); c.setQty1(null); expectBad(c, "수량1");
        c = in(); c.setQty1("0"); expectBad(c, "수량1");
        c = in(); c.setQty1("abc"); expectBad(c, "수량1");
        c = in(); c.setQty1("123456"); expectBad(c, "수량1");
        c = in(); c.setCapa2("2kg"); expectBad(c, "함께");      // 용량2만
        c = in(); c.setQty2("2"); expectBad(c, "함께");          // 수량2만
        c = in(); c.setUseYn(2); expectBad(c, "사용여부");
        c = in(); c.setShopCode("1".repeat(13)); expectBad(c, "상점");
        c = in(); c.setKeywrd("가".repeat(51)); expectBad(c, "키워드");
    }

    @Test
    void 수정은_상점_상품코드_상품명칭_사용여부만_바꾸고_키워드와_용량은_그대로다() {
        OrderProdCode existing = in();
        existing.setMatchProdCode("황기 닭백숙 삼계탕::1kgx3");
        existing.setUseYn(1);
        when(mapper.select(Map.of("matchProdCode", "황기 닭백숙 삼계탕::1kgx3"))).thenReturn(existing);
        OrderProdCode req = new OrderProdCode();
        req.setMatchProdCode("황기 닭백숙 삼계탕::1kgx3");
        req.setShopCode("3018508386A1");
        req.setProdCode("new-code");
        req.setProdName("새 이름");
        req.setUseYn(0);
        req.setKeywrd("바꾸려는 키워드");   // 무시
        req.setCapa1("9kg");               // 무시
        req.setChangeId("u2");
        req.setChangeName("수정자");

        service.modify(req);

        ArgumentCaptor<OrderProdCode> cap = ArgumentCaptor.forClass(OrderProdCode.class);
        verify(mapper).update(cap.capture());
        OrderProdCode row = cap.getValue();
        assertEquals("3018508386A1", row.getShopCode());
        assertEquals("new-code", row.getProdCode());
        assertEquals("새 이름", row.getProdName());
        assertEquals(0, row.getUseYn());
        assertEquals("황기 닭백숙 삼계탕", row.getKeywrd());
        assertEquals("1kg", row.getCapa1());
        assertEquals("수정", row.getResultType());
        assertEquals("u2", row.getChangeId());
    }

    @Test
    void 없는_코드를_수정_삭제하면_400이고_삭제는_그_행만_지운다() {
        OrderProdCode req = new OrderProdCode();
        req.setMatchProdCode("없는코드");
        req.setShopCode("s"); req.setProdCode("p"); req.setProdName("n");
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.modify(req)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.remove("없는코드")).getStatusCode().value());

        OrderProdCode existing = in();
        existing.setMatchProdCode("있는코드");
        when(mapper.select(Map.of("matchProdCode", "있는코드"))).thenReturn(existing);
        service.remove("있는코드");
        verify(mapper).delete(existing);
    }
}
