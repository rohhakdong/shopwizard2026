package com.shopwizard.order.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import com.shopwizard.order.mapper.OrderHistoryMapper;
import com.shopwizard.order.model.OrderConslt;
import com.shopwizard.order.model.OrderConsltRequest;
import com.shopwizard.order.service.OrderConsltService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 주문상세의 상담·내역 — 상점 제한 계정은 상담을 볼 수 없고, 그 밖의 제한 계정은 자기 범위의 주문만. */
class OrderConsltControllerTest {

    private DataScopeMapper scopeMapper;
    private OrderConsltService service;
    private OrderConsltController controller;
    private OrderHistoryMapper historyMapper;
    private OrderHistoryController historyController;

    @BeforeEach
    void setUp() {
        scopeMapper = mock(DataScopeMapper.class);
        DataScopeGuard guard = new DataScopeGuard(scopeMapper);
        ReflectionTestUtils.setField(guard, "mode", "enforce");
        service = mock(OrderConsltService.class);
        controller = new OrderConsltController(service, guard);
        historyMapper = mock(OrderHistoryMapper.class);
        historyController = new OrderHistoryController(historyMapper, guard);
    }

    private static MockHttpServletRequest login(Map<String, String> claims) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, claims);
        AuthSession.markActor(req, AuthSession.Type.MNGR);
        return req;
    }

    private static MockHttpServletRequest admin() { return login(Map.of("svcAll", "Y", "loginId", "u", "name", "관리자")); }
    private static MockHttpServletRequest shopUser() { return login(Map.of("svcAll", "Y", "loginId", "u", "shopCode", "SHOP1")); }
    private static MockHttpServletRequest chnlUser() { return login(Map.of("svcAll", "Y", "loginId", "u", "chnlCode", "MINE")); }

    private static OrderConsltRequest post(int orderNo) {
        OrderConsltRequest r = new OrderConsltRequest();
        r.setOrderNo(orderNo);
        return r;
    }

    @Test
    void 제한_없는_계정은_그대로_서비스로_간다() {
        when(service.list(10)).thenReturn(List.of(new OrderConslt()));
        OrderConsltRequest r = post(10);

        assertEquals(1, controller.list(10, admin()).size());
        controller.register(r, admin());
        verify(service).register(eq(r), any());
    }

    @Test
    void 상점_제한_계정은_상담을_조회_등록_수정_모두_403_이고_서비스를_부르지_않는다() {
        assertThrows(ResponseStatusException.class, () -> controller.list(10, shopUser()));
        assertThrows(ResponseStatusException.class, () -> controller.register(post(10), shopUser()));
        OrderConsltRequest put = new OrderConsltRequest();
        put.setCustConsltNo(5);
        assertThrows(ResponseStatusException.class, () -> controller.answer(put, shopUser()));
        verifyNoInteractions(service);
    }

    @Test
    void 채널_제한_계정은_남의_주문_상담을_403_내_주문은_통과한다() {
        when(scopeMapper.countOrder(10, null, "MINE", null)).thenReturn(1);
        when(scopeMapper.countOrder(11, null, "MINE", null)).thenReturn(0);

        controller.list(10, chnlUser());
        controller.register(post(10), chnlUser());
        verify(service).list(10);
        assertThrows(ResponseStatusException.class, () -> controller.list(11, chnlUser()));
        assertThrows(ResponseStatusException.class, () -> controller.register(post(11), chnlUser()));
        verify(service, never()).list(11);
        verify(service, times(1)).register(any(), any());
    }

    @Test
    void 처리_수정은_상담이_가리키는_주문으로_범위를_검사한다() {
        OrderConslt mine = new OrderConslt();
        mine.setOrderNo("10");
        OrderConslt theirs = new OrderConslt();
        theirs.setOrderNo("11");
        OrderConslt noOrder = new OrderConslt();
        noOrder.setOrderNo("abc");
        when(service.find(1)).thenReturn(mine);
        when(service.find(2)).thenReturn(theirs);
        when(service.find(3)).thenReturn(noOrder);
        when(scopeMapper.countOrder(10, null, "MINE", null)).thenReturn(1);
        when(scopeMapper.countOrder(11, null, "MINE", null)).thenReturn(0);

        OrderConsltRequest ok = new OrderConsltRequest();
        ok.setCustConsltNo(1);
        controller.answer(ok, chnlUser());
        verify(service).answer(eq(ok), any());

        for (int no : new int[]{2, 3, 4}) {   // 남의 주문 / 주문번호가 숫자가 아님 / 없는 상담
            OrderConsltRequest r = new OrderConsltRequest();
            r.setCustConsltNo(no);
            assertThrows(ResponseStatusException.class, () -> controller.answer(r, chnlUser()), "상담 " + no);
        }
        verify(service, times(1)).answer(any(), any());
    }

    @Test
    void 제한_없는_계정이_없는_상담을_고치면_서비스가_400_으로_알린다() {
        OrderConsltRequest r = new OrderConsltRequest();
        r.setCustConsltNo(99);
        when(service.answer(eq(r), any())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "상담을 찾을 수 없습니다."));

        assertEquals(400, assertThrows(ResponseStatusException.class, () -> controller.answer(r, admin())).getStatusCode().value());
    }

    // ── 배송·반품·결제 내역 ────────────────────────────────

    @Test
    void 내역은_주문_범위를_검사한_뒤_출고_반품_결제를_함께_돌려준다() {
        when(historyMapper.selectShips(10)).thenReturn(List.of(Map.of("orderProdNo", 1)));
        when(historyMapper.selectReturns(10)).thenReturn(List.of());
        when(historyMapper.selectPays(10)).thenReturn(List.of(Map.of("payNo", 1)));

        Map<String, Object> r = historyController.history(10, admin());

        assertEquals(List.of("ships", "returns", "pays"), List.copyOf(r.keySet()));
        assertEquals(1, ((List<?>) r.get("ships")).size());
        assertEquals(1, ((List<?>) r.get("pays")).size());
    }

    @Test
    void 내역도_남의_주문이면_403_이고_상점_계정은_자기_상점_주문만_볼_수_있다() {
        when(scopeMapper.countOrder(10, null, "MINE", null)).thenReturn(1);
        when(scopeMapper.countOrder(11, null, "MINE", null)).thenReturn(0);
        when(scopeMapper.countOrder(10, "SHOP1", null, null)).thenReturn(1);

        historyController.history(10, chnlUser());
        assertThrows(ResponseStatusException.class, () -> historyController.history(11, chnlUser()));
        historyController.history(10, shopUser());   // 상점 계정도 자기 주문 내역(배송·결제)은 본다 — 막는 것은 상담뿐
        verify(historyMapper, never()).selectShips(11);
    }
}
