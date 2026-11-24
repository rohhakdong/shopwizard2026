package com.shopwizard.order.service;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.order.mapper.OrderConsltMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderConslt;
import com.shopwizard.order.model.OrderConsltRequest;
import com.shopwizard.order.model.OrderProd;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 주문상세의 고객상담 — 등록(서버가 채우는 값·검증·상담여부)과 처리내용 수정. */
class OrderConsltServiceTest {

    private final OrderConsltMapper mapper = mock(OrderConsltMapper.class);
    private final OrderService orderService = mock(OrderService.class);
    private final OrderProdMapper orderProdMapper = mock(OrderProdMapper.class);
    private final OrderConsltService service = new OrderConsltService(mapper, orderService, orderProdMapper);
    private final AuthSession.Actor actor = new AuthSession.Actor("rohhd", "노학동");

    @BeforeEach
    void setUp() {
        Order order = new Order();
        order.setOrderNo(2415372);
        order.setCustId(7);
        order.setOrderName("홍길동");
        order.setOrderMobileNo("010-1111-2222");
        order.setOrderPhoneNo("02-123-4567");
        order.setOrderEmail("hong@example.com");
        order.setChnlCode("CH1");
        when(orderService.select(any())).thenReturn(order);
        when(mapper.countActiveCode(anyString(), anyString())).thenReturn(1);
        doAnswer(inv -> {
            ((OrderConslt) inv.getArgument(0)).setCustConsltNo(901);
            return 1;
        }).when(mapper).insert(any());
        OrderConslt saved = new OrderConslt();
        saved.setCustConsltNo(901);
        when(mapper.selectOne(901)).thenReturn(saved);
    }

    private static OrderConsltRequest req() {
        OrderConsltRequest r = new OrderConsltRequest();
        r.setOrderNo(2415372);
        r.setCustConsltType("order");
        r.setCustConsltReasonType("etc");
        r.setConsltCntnts("배송 일정 문의");
        return r;
    }

    private void expectBad(OrderConsltRequest r, String messagePart) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.register(r, actor));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains(messagePart), ex.getReason());
        verify(mapper, never()).insert(any());
        verify(mapper, never()).markConsulted(anyInt(), any(), anyString(), anyString());
    }

    // ── 등록 ────────────────────────────────────────────────

    @Test
    void 주문_전체_상담은_고객_정보와_채널을_주문_헤더에서_채우고_모든_줄을_상담여부로_표시한다() {
        OrderConslt saved = service.register(req(), actor);

        assertEquals(901, saved.getCustConsltNo());
        ArgumentCaptor<OrderConslt> cap = ArgumentCaptor.forClass(OrderConslt.class);
        verify(mapper).insert(cap.capture());
        OrderConslt c = cap.getValue();
        assertEquals("2415372", c.getOrderNo());
        assertNull(c.getOrderProdNo());
        assertEquals("order", c.getCustConsltType());
        assertEquals("etc", c.getCustConsltReasonType());
        assertNull(c.getConsltMediaType());
        assertEquals(7, c.getCustId());
        assertEquals("홍길동", c.getCustName());
        assertEquals("010-1111-2222", c.getCustMobileNo());
        assertEquals("02-123-4567", c.getCustPhoneNo());
        assertEquals("hong@example.com", c.getCustEmail());
        assertEquals("CH1", c.getChnlCode());
        assertEquals("배송 일정 문의", c.getConsltCntnts());
        assertEquals("배송 일정 문의", c.getConsltTitle());
        assertEquals("rohhd", c.getMngrId());
        assertEquals("노학동", c.getMngrName());
        assertEquals("rohhd", c.getRegistId());
        assertNull(c.getAnswrCntnts());
        verify(mapper).markConsulted(2415372, null, "rohhd", "노학동");
    }

    @Test
    void 줄을_고르면_그_줄의_상품_정보를_담고_그_줄만_상담여부로_표시한다() {
        OrderProd line = new OrderProd();
        line.setProdCode("1000000342");
        line.setItemCode("20000");
        line.setItemName("선택사항없음");
        when(orderProdMapper.selectLineRaw(Map.of("orderNo", 2415372, "orderProdNo", 2))).thenReturn(line);
        OrderConsltRequest r = req();
        r.setOrderProdNo(2);
        r.setConsltMediaType("phone");

        service.register(r, actor);

        ArgumentCaptor<OrderConslt> cap = ArgumentCaptor.forClass(OrderConslt.class);
        verify(mapper).insert(cap.capture());
        assertEquals("2", cap.getValue().getOrderProdNo());
        assertEquals("1000000342", cap.getValue().getProdCode());
        assertEquals("20000", cap.getValue().getItemCode());
        assertEquals("phone", cap.getValue().getConsltMediaType());
        verify(mapper).markConsulted(2415372, 2, "rohhd", "노학동");
        verify(mapper).countActiveCode("cConsltMediaType", "phone");
    }

    @Test
    void 제목은_내용_앞_50자이고_처리내용을_함께_적으면_처리_제목도_채운다() {
        OrderConsltRequest r = req();
        r.setConsltCntnts("가".repeat(80));
        r.setAnswrCntnts("나".repeat(70));
        r.setCustName("김철수");
        r.setCustMobileNo("010-9999-8888");

        service.register(r, actor);

        ArgumentCaptor<OrderConslt> cap = ArgumentCaptor.forClass(OrderConslt.class);
        verify(mapper).insert(cap.capture());
        OrderConslt c = cap.getValue();
        assertEquals("가".repeat(50), c.getConsltTitle());
        assertEquals("가".repeat(80), c.getConsltCntnts());
        assertEquals("나".repeat(70), c.getAnswrCntnts());
        assertEquals("나".repeat(50), c.getAnswrTitle());
        assertEquals("김철수", c.getCustName());          // 입력이 있으면 입력값
        assertEquals("010-9999-8888", c.getCustMobileNo());
        assertEquals("02-123-4567", c.getCustPhoneNo());  // 안 적은 칸은 주문자 값
    }

    @Test
    void 필수값이_비거나_코드표에_없으면_거절하고_아무것도_저장하지_않는다() {
        OrderConsltRequest noOrder = req();
        noOrder.setOrderNo(null);
        expectBad(noOrder, "주문번호");

        OrderConsltRequest noType = req();
        noType.setCustConsltType(" ");
        expectBad(noType, "상담구분");

        OrderConsltRequest noReason = req();
        noReason.setCustConsltReasonType(null);
        expectBad(noReason, "상담사유");

        OrderConsltRequest noContent = req();
        noContent.setConsltCntnts("  ");
        expectBad(noContent, "상담내용");

        when(mapper.countActiveCode("cCustConsltType", "cancel")).thenReturn(0);   // 사용 중지된 코드
        OrderConsltRequest badType = req();
        badType.setCustConsltType("cancel");
        expectBad(badType, "상담구분이 올바르지");

        when(mapper.countActiveCode("cCustConsltReasonType", "zzz")).thenReturn(0);
        OrderConsltRequest badReason = req();
        badReason.setCustConsltReasonType("zzz");
        expectBad(badReason, "상담사유가 올바르지");

        when(mapper.countActiveCode("cConsltMediaType", "fax")).thenReturn(0);
        OrderConsltRequest badMedia = req();
        badMedia.setConsltMediaType("fax");
        expectBad(badMedia, "상담매체");
    }

    @Test
    void 글자수_제한을_넘으면_거절한다() {
        OrderConsltRequest longContent = req();
        longContent.setConsltCntnts("가".repeat(OrderConsltService.MAX_TEXT + 1));
        expectBad(longContent, "상담내용");

        OrderConsltRequest longAnswer = req();
        longAnswer.setAnswrCntnts("가".repeat(OrderConsltService.MAX_TEXT + 1));
        expectBad(longAnswer, "처리내용");
    }

    @Test
    void 없는_주문이거나_이_주문에_없는_줄이면_거절한다() {
        when(orderService.select(any())).thenReturn(null);
        expectBad(req(), "주문을 찾을 수 없습니다");

        when(orderService.select(any())).thenReturn(new Order());
        when(orderProdMapper.selectLineRaw(any())).thenReturn(null);
        OrderConsltRequest r = req();
        r.setOrderProdNo(9);
        expectBad(r, "없는 상품 줄");
    }

    @Test
    void 로그인_정보가_없으면_담당자는_SYSTEM_이다() {
        service.register(req(), null);

        ArgumentCaptor<OrderConslt> cap = ArgumentCaptor.forClass(OrderConslt.class);
        verify(mapper).insert(cap.capture());
        assertEquals("SYSTEM", cap.getValue().getMngrId());
    }

    // ── 처리내용 수정 ───────────────────────────────────────

    @Test
    void 처리내용은_제목_날짜_담당만_바꾼다() {
        OrderConslt existing = new OrderConslt();
        existing.setCustConsltNo(55);
        when(mapper.selectOne(55)).thenReturn(existing);
        OrderConsltRequest r = new OrderConsltRequest();
        r.setCustConsltNo(55);
        r.setAnswrCntnts("다" .repeat(60));
        r.setConsltCntnts("바꾸려는 내용");   // 무시된다
        r.setCustConsltType("etc");            // 무시된다

        service.answer(r, actor);

        verify(mapper).updateAnswer(55, "다".repeat(50), "다".repeat(60), "rohhd", "노학동", "rohhd", "노학동");
        verify(mapper, never()).insert(any());
        verify(mapper, never()).markConsulted(anyInt(), any(), anyString(), anyString());
    }

    @Test
    void 처리내용이_비었거나_상담이_없으면_거절한다() {
        OrderConsltRequest empty = new OrderConsltRequest();
        empty.setCustConsltNo(55);
        empty.setAnswrCntnts(" ");
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.answer(empty, actor)).getStatusCode().value());

        OrderConsltRequest missing = new OrderConsltRequest();
        missing.setCustConsltNo(404);
        missing.setAnswrCntnts("내용");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.answer(missing, actor));
        assertTrue(ex.getReason().contains("상담을 찾을 수 없습니다"));

        OrderConsltRequest noNo = new OrderConsltRequest();
        noNo.setAnswrCntnts("내용");
        assertThrows(ResponseStatusException.class, () -> service.answer(noNo, actor));
        verify(mapper, never()).updateAnswer(anyInt(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void 목록은_주문번호_문자열로_조회한다() {
        when(mapper.selectByOrder("2415372")).thenReturn(List.of(new OrderConslt()));

        assertEquals(1, service.list(2415372).size());
        verify(mapper).selectByOrder(eq("2415372"));
    }
}
