package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderFeeRuleMapper;
import com.shopwizard.order.model.OrderFeeRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** 수수료 규칙 — 검증(배송형태·수수료율·구간)과 값 정리, 수정에서 사용 여부 유지, 없는 규칙 400. */
class OrderFeeRuleServiceTest {

    private final OrderFeeRuleMapper mapper = mock(OrderFeeRuleMapper.class);
    private final OrderFeeRuleService service = new OrderFeeRuleService(mapper);

    @BeforeEach
    void setUp() {
        when(mapper.countShop(anyString())).thenReturn(1);
        when(mapper.countChnl(anyString())).thenReturn(1);
        when(mapper.select(any())).thenReturn(null);
        doAnswer(inv -> { ((OrderFeeRule) inv.getArgument(0)).setRuleNo(9); return 1; }).when(mapper).insert(any());
    }

    private static OrderFeeRule in() {
        OrderFeeRule r = new OrderFeeRule();
        r.setShopCode(" 2148817433A1 ");
        r.setChnlCode("1208800767Z1");
        r.setDeliFeeSect("선불");
        r.setDeliFee(2500);
        r.setPromotFee(500);
        r.setOrderAmtStart(0);
        r.setOrderAmtEnd(79999);
        r.setRegistId("u");
        r.setRegistName("사용자");
        return r;
    }

    private void expectBad(OrderFeeRule r, String part) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(r));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains(part), ex.getReason());
        verify(mapper, never()).insert(any());
    }

    @Test
    void 등록하면_값을_정리해_저장하고_비운_조건은_빈_문자열_구간은_0_사용_중으로_시작한다() {
        OrderFeeRule saved = new OrderFeeRule();
        saved.setRuleNo(9);
        when(mapper.select(Map.of("ruleNo", 9))).thenReturn(saved);

        OrderFeeRule result = service.create(in());

        assertEquals(9, result.getRuleNo());
        ArgumentCaptor<OrderFeeRule> cap = ArgumentCaptor.forClass(OrderFeeRule.class);
        verify(mapper).insert(cap.capture());
        OrderFeeRule row = cap.getValue();
        assertEquals("2148817433A1", row.getShopCode());
        assertEquals("", row.getProdName());
        assertEquals("", row.getDealName());
        assertEquals(0, row.getOrderQtyStart());
        assertEquals(0, row.getOrderQtyEnd());
        assertEquals(2500, row.getDeliFee());
        assertEquals(500, row.getPromotFee());
        assertEquals(0, new BigDecimal("0.00").compareTo(row.getChnlFeeRate()));
        assertEquals(1, row.getState());
        assertEquals("u", row.getRegistId());
    }

    @Test
    void 수수료율이_있으면_배송비_판촉비를_0_배송형태를_전체무료로_정리한다() {
        OrderFeeRule r = in();
        r.setChnlFeeRate(new BigDecimal("9.9"));

        service.create(r);

        ArgumentCaptor<OrderFeeRule> cap = ArgumentCaptor.forClass(OrderFeeRule.class);
        verify(mapper).insert(cap.capture());
        assertEquals(0, cap.getValue().getDeliFee());
        assertEquals(0, cap.getValue().getPromotFee());
        assertEquals("전체무료", cap.getValue().getDeliFeeSect());
        assertEquals(0, new BigDecimal("9.90").compareTo(cap.getValue().getChnlFeeRate()));
    }

    @Test
    void 필수값과_범위를_검사한다() {
        OrderFeeRule r = in(); r.setShopCode(" "); expectBad(r, "상점");
        r = in(); r.setDeliFeeSect("택배"); expectBad(r, "배송형태");
        r = in(); r.setDeliFeeSect(null); expectBad(r, "배송형태");
        r = in(); r.setChnlFeeRate(new BigDecimal("100.5")); expectBad(r, "수수료율");
        r = in(); r.setChnlFeeRate(new BigDecimal("-1")); expectBad(r, "수수료율");
        r = in(); r.setDeliFee(-1); expectBad(r, "배송비");
        r = in(); r.setPromotFee(-5); expectBad(r, "판촉비");
        r = in(); r.setOrderQtyStart(10); r.setOrderQtyEnd(3); expectBad(r, "주문수량");
        r = in(); r.setOrderAmtStart(9000); r.setOrderAmtEnd(5000); expectBad(r, "주문금액");
        r = in(); r.setProdName("가".repeat(51)); expectBad(r, "상품명");
        r = in(); r.setRemark("가".repeat(51)); expectBad(r, "비고");
        r = in(); r.setState(2); expectBad(r, "사용 여부");
    }

    @Test
    void 구간_종료가_0이면_제한_없음() {
        OrderFeeRule r = in(); r.setOrderQtyStart(5); r.setOrderQtyEnd(0);

        service.create(r);

        verify(mapper).insert(any());
    }

    @Test
    void 없는_상점_채널이면_400이고_채널을_비우면_채널_확인을_하지_않는다() {
        when(mapper.countShop("NOPE")).thenReturn(0);
        OrderFeeRule r = in(); r.setShopCode("NOPE"); expectBad(r, "없는 상점");
        when(mapper.countChnl("NOCH")).thenReturn(0);
        r = in(); r.setChnlCode("NOCH"); expectBad(r, "없는 채널");

        reset(mapper);
        when(mapper.countShop(anyString())).thenReturn(1);
        OrderFeeRule noChnl = in(); noChnl.setChnlCode(null);
        service.create(noChnl);
        verify(mapper, never()).countChnl(anyString());
    }

    @Test
    void 수정에서_사용_여부를_비우면_지금_값을_유지하고_값을_주면_바꾼다() {
        OrderFeeRule e = new OrderFeeRule();
        e.setRuleNo(3);
        e.setState(0);
        when(mapper.select(Map.of("ruleNo", 3))).thenReturn(e);
        OrderFeeRule keep = in(); keep.setRuleNo(3);

        service.modify(keep);
        ArgumentCaptor<OrderFeeRule> cap = ArgumentCaptor.forClass(OrderFeeRule.class);
        verify(mapper).update(cap.capture());
        assertEquals(0, cap.getValue().getState());
        clearInvocations(mapper);

        OrderFeeRule on = in(); on.setRuleNo(3); on.setState(1);
        service.modify(on);
        verify(mapper).update(cap.capture());
        assertEquals(1, cap.getValue().getState());
    }

    @Test
    void 없는_규칙을_수정_삭제하면_400이고_삭제는_그_행만_지운다() {
        OrderFeeRule req = in(); req.setRuleNo(99);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.modify(req)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.remove(99)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.remove(null)).getStatusCode().value());

        OrderFeeRule e = new OrderFeeRule(); e.setRuleNo(3);
        when(mapper.select(Map.of("ruleNo", 3))).thenReturn(e);
        service.remove(3);
        verify(mapper).delete(e);
    }
}
