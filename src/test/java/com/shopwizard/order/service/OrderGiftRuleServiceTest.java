package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderGiftRuleMapper;
import com.shopwizard.order.model.OrderGiftRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** 발주 사은품 규칙 — 검증(구간·기간·사은품 쌍)·값 정리·마스터 존재 확인·수정에서 기존 코드 허용. */
class OrderGiftRuleServiceTest {

    private final OrderGiftRuleMapper mapper = mock(OrderGiftRuleMapper.class);
    private final OrderGiftRuleService service = new OrderGiftRuleService(mapper);

    @BeforeEach
    void setUp() {
        when(mapper.countShop(anyString())).thenReturn(1);
        when(mapper.countChnl(anyString())).thenReturn(1);
        when(mapper.countUprice(anyString())).thenReturn(1);
        when(mapper.select(any())).thenReturn(null);
        doAnswer(inv -> { ((OrderGiftRule) inv.getArgument(0)).setRuleNo(7); return 1; }).when(mapper).insert(any());
    }

    private static OrderGiftRule in() {
        OrderGiftRule r = new OrderGiftRule();
        r.setShopCode(" 2148817433A1 ");
        r.setChnlCode("1048636968Z1");
        r.setChnlProdName(" 매트 ");
        r.setGiftProdCode1("증정마스크-소");
        r.setGiftProdName1("힣마스크(5매입)");
        r.setGiftQty(1);
        r.setTotalOrderQtyStart(2);
        r.setTotalOrderQtyEnd(999);
        r.setRegistId("u");
        r.setRegistName("사용자");
        return r;
    }

    private void expectBad(OrderGiftRule r, String messagePart) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(r));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains(messagePart), ex.getReason());
        verify(mapper, never()).insert(any());
    }

    @Test
    void 등록하면_값을_정리해_저장하고_빈_조건은_빈_문자열_구간은_0이다() {
        OrderGiftRule saved = new OrderGiftRule();
        saved.setRuleNo(7);
        when(mapper.select(Map.of("ruleNo", 7))).thenReturn(saved);

        OrderGiftRule result = service.create(in());

        assertEquals(7, result.getRuleNo());
        ArgumentCaptor<OrderGiftRule> cap = ArgumentCaptor.forClass(OrderGiftRule.class);
        verify(mapper).insert(cap.capture());
        OrderGiftRule row = cap.getValue();
        assertEquals("2148817433A1", row.getShopCode());
        assertEquals("매트", row.getChnlProdName());
        assertEquals("", row.getChnlProdCode());
        assertEquals("", row.getDealName());
        assertEquals(0, row.getTotalOrderAmtStart());
        assertEquals(0, row.getTotalOrderAmtEnd());
        assertEquals(0, row.getPromotFee());
        assertEquals(0, row.getDupYn());
        assertEquals("", row.getGiftProdCode2());
        assertNull(row.getGiftStartDate());
        assertEquals(1, row.getState());
        assertEquals("u", row.getRegistId());
    }

    @Test
    void 증정_기간은_시작_0시_종료_23시59분59초로_정규화한다_시각이_붙어_와도_날짜만_본다() {
        OrderGiftRule r = in();
        r.setGiftStartDate("2021-08-13");
        r.setGiftEndDate("2021-08-13 00:00:00");

        service.create(r);

        ArgumentCaptor<OrderGiftRule> cap = ArgumentCaptor.forClass(OrderGiftRule.class);
        verify(mapper).insert(cap.capture());
        assertEquals("2021-08-13 00:00:00", cap.getValue().getGiftStartDate());
        assertEquals("2021-08-13 23:59:59", cap.getValue().getGiftEndDate());
    }

    @Test
    void 필수값과_범위를_검사한다() {
        OrderGiftRule r = in(); r.setShopCode(" "); expectBad(r, "상점");
        r = in(); r.setGiftProdCode1(null); expectBad(r, "사은품 코드 1");
        r = in(); r.setGiftProdName1(""); expectBad(r, "사은품 명칭 1");
        r = in(); r.setGiftQty(null); expectBad(r, "증정 수량");
        r = in(); r.setGiftQty(0); expectBad(r, "증정 수량");
        r = in(); r.setPromotFee(-1); expectBad(r, "판촉 수수료");
        r = in(); r.setDupYn(-1); expectBad(r, "중복 여부");
        r = in(); r.setTotalOrderQtyStart(-1); expectBad(r, "총 주문수량 시작");
        r = in(); r.setDealName("가".repeat(51)); expectBad(r, "딜명칭");
        r = in(); r.setChnlProdName("가".repeat(256)); expectBad(r, "채널상품명칭");
        r = in(); r.setRemark("가".repeat(51)); expectBad(r, "비고");
    }

    @Test
    void 구간은_종료가_0이면_제한_없음_아니면_시작이_종료보다_클_수_없다() {
        OrderGiftRule open = in(); open.setTotalOrderQtyStart(5); open.setTotalOrderQtyEnd(0);
        service.create(open);   // 5 이상, 상한 없음
        verify(mapper).insert(any());
        clearInvocations(mapper);

        OrderGiftRule r = in(); r.setTotalOrderQtyStart(10); r.setTotalOrderQtyEnd(3); expectBad(r, "총 주문수량");
        r = in(); r.setTotalOrderAmtStart(10000); r.setTotalOrderAmtEnd(5000); expectBad(r, "총 주문금액");
    }

    @Test
    void 증정_기간은_함께_입력해야_하고_종료가_빠를_수_없고_날짜_형식이어야_한다() {
        OrderGiftRule r = in(); r.setGiftStartDate("2021-08-13"); expectBad(r, "함께");
        r = in(); r.setGiftEndDate("2021-08-13"); expectBad(r, "함께");
        r = in(); r.setGiftStartDate("2021-08-14"); r.setGiftEndDate("2021-08-13"); expectBad(r, "빠를 수 없");
        r = in(); r.setGiftStartDate("2021-13-45"); r.setGiftEndDate("2021-08-13"); expectBad(r, "날짜");
    }

    @Test
    void 사은품_2에서_5는_코드와_명칭을_함께_입력해야_한다() {
        OrderGiftRule r = in(); r.setGiftProdCode2("A-1"); expectBad(r, "사은품 2");
        r = in(); r.setGiftProdName5("이름만"); expectBad(r, "사은품 5");

        OrderGiftRule ok = in(); ok.setGiftProdCode3("A-1"); ok.setGiftProdName3("힣사은품");
        service.create(ok);
        verify(mapper).insert(any());
    }

    @Test
    void 없는_상점_채널이면_400_사은품_코드_1_이_발주단가표에_없어도_400() {
        when(mapper.countShop("NOPE")).thenReturn(0);
        OrderGiftRule r = in(); r.setShopCode("NOPE"); expectBad(r, "없는 상점");

        when(mapper.countChnl("NOCHNL")).thenReturn(0);
        r = in(); r.setChnlCode("NOCHNL"); expectBad(r, "없는 채널");

        when(mapper.countUprice("없는코드")).thenReturn(0);
        r = in(); r.setGiftProdCode1("없는코드"); expectBad(r, "발주단가표에 없습니다");
    }

    @Test
    void 채널을_비우면_채널_확인을_하지_않는다() {
        OrderGiftRule r = in(); r.setChnlCode(null);

        service.create(r);

        verify(mapper, never()).countChnl(anyString());
    }

    @Test
    void 사용_여부는_등록에서_비우면_사용_중_수정에서_비우면_지금_값_유지_0_1이_아니면_400() {
        service.create(in());
        ArgumentCaptor<OrderGiftRule> cap = ArgumentCaptor.forClass(OrderGiftRule.class);
        verify(mapper).insert(cap.capture());
        assertEquals(1, cap.getValue().getState());
        clearInvocations(mapper);

        OrderGiftRule off = in(); off.setState(0);
        service.create(off);
        verify(mapper).insert(cap.capture());
        assertEquals(0, cap.getValue().getState());
        clearInvocations(mapper);

        OrderGiftRule bad = in(); bad.setState(2);
        expectBad(bad, "사용 여부");
    }

    @Test
    void 수정에서_사용_여부를_바꾸거나_비우면_기존_값을_지킨다() {
        OrderGiftRule e = existing();   // 레거시 규칙처럼 State=0 으로 가정
        e.setState(0);
        OrderGiftRule keep = in(); keep.setRuleNo(3); keep.setGiftProdCode1("힣증정고농축피죤-175");
        service.modify(keep);
        ArgumentCaptor<OrderGiftRule> cap = ArgumentCaptor.forClass(OrderGiftRule.class);
        verify(mapper).update(cap.capture());
        assertEquals(0, cap.getValue().getState());   // 비우면 그대로 — 저장했다고 레거시 규칙이 갑자기 켜지지 않는다
        clearInvocations(mapper);

        OrderGiftRule on = in(); on.setRuleNo(3); on.setGiftProdCode1("힣증정고농축피죤-175"); on.setState(1);
        service.modify(on);
        verify(mapper).update(cap.capture());
        assertEquals(1, cap.getValue().getState());
    }

    private OrderGiftRule existing() {
        OrderGiftRule e = in();
        e.setRuleNo(3);
        e.setShopCode("2148817433A1");
        e.setGiftProdCode1("힣증정고농축피죤-175");   // 레거시 값: 발주단가표에 없다
        when(mapper.select(Map.of("ruleNo", 3))).thenReturn(e);
        when(mapper.countUprice("힣증정고농축피죤-175")).thenReturn(0);
        return e;
    }

    @Test
    void 수정에서_사은품_코드를_바꾸지_않으면_레거시_코드도_통과한다() {
        existing();
        OrderGiftRule req = in();
        req.setRuleNo(3);
        req.setGiftProdCode1("힣증정고농축피죤-175");
        req.setGiftQty(2);
        req.setChangeId("u2");
        req.setChangeName("수정자");

        service.modify(req);

        ArgumentCaptor<OrderGiftRule> cap = ArgumentCaptor.forClass(OrderGiftRule.class);
        verify(mapper).update(cap.capture());
        assertEquals(3, cap.getValue().getRuleNo());
        assertEquals(2, cap.getValue().getGiftQty());
        assertEquals("u2", cap.getValue().getChangeId());
    }

    @Test
    void 수정에서_사은품_코드를_바꾸면_발주단가표에_있어야_한다() {
        existing();
        when(mapper.countUprice("없는코드")).thenReturn(0);
        OrderGiftRule req = in();
        req.setRuleNo(3);
        req.setGiftProdCode1("없는코드");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.modify(req));

        assertEquals(400, ex.getStatusCode().value());
        verify(mapper, never()).update(any());
    }

    @Test
    void 없는_규칙을_수정_삭제하면_400이고_삭제는_그_행만_지운다() {
        OrderGiftRule req = in();
        req.setRuleNo(99);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.modify(req)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.remove(99)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.remove(null)).getStatusCode().value());

        OrderGiftRule e = existing();
        service.remove(3);
        verify(mapper).delete(e);
    }
}
