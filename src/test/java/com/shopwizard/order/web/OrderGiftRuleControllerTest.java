package com.shopwizard.order.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import com.shopwizard.order.model.OrderGiftRule;
import com.shopwizard.order.service.OrderGiftRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 사은품규칙의 데이터 범위 — 상점 제한은 자기 상점 규칙만, 채널 제한은 불가, 서비스 제한은 자기 서비스의 상점·채널만. */
class OrderGiftRuleControllerTest {

    private DataScopeMapper mapper;
    private OrderGiftRuleService service;
    private OrderGiftRuleController controller;

    @BeforeEach
    void setUp() {
        mapper = mock(DataScopeMapper.class);
        DataScopeGuard scopeGuard = new DataScopeGuard(mapper);
        ReflectionTestUtils.setField(scopeGuard, "mode", "enforce");
        service = mock(OrderGiftRuleService.class);
        controller = new OrderGiftRuleController(service, new OrderMasterGuard(scopeGuard), scopeGuard);
    }

    private static MockHttpServletRequest login(Map<String, String> claims) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, claims);
        return req;
    }

    private static final Map<String, String> ALL = Map.of("svcAll", "Y", "loginId", "u");
    private static final Map<String, String> SHOP = Map.of("svcAll", "Y", "loginId", "u", "shopCode", "SHOP1");
    private static final Map<String, String> CHNL = Map.of("svcAll", "Y", "loginId", "u", "chnlCode", "CH1");
    private static final Map<String, String> SVC = Map.of("loginId", "u", "svcCode", "SHP001");

    private static OrderGiftRule rule(Integer no, String shop, String chnl) {
        OrderGiftRule r = new OrderGiftRule();
        r.setRuleNo(no);
        r.setShopCode(shop);
        r.setChnlCode(chnl);
        return r;
    }

    @Test
    void 제한_없는_계정은_그대로_서비스로_간다() {
        OrderGiftRule body = rule(null, "ANY", "ANYCH");

        controller.create(body, login(ALL));

        verify(service).create(body);
    }

    @Test
    void 상점_제한_계정은_남의_상점_규칙을_조회_수정_삭제할_수_없다() {
        when(service.find(5)).thenReturn(rule(5, "SHOP2", ""));
        MockHttpServletRequest shop = login(SHOP);

        assertThrows(ResponseStatusException.class, () -> controller.select(5, shop));
        assertThrows(ResponseStatusException.class, () -> controller.remove(5, shop));
        assertThrows(ResponseStatusException.class, () -> controller.modify(rule(5, "SHOP1", ""), shop));
        verify(service, never()).remove(any());
        verify(service, never()).modify(any());
    }

    @Test
    void 상점_제한_계정은_자기_규칙을_다른_상점으로_옮기거나_다른_상점에_등록할_수_없다() {
        when(service.find(5)).thenReturn(rule(5, "SHOP1", ""));
        MockHttpServletRequest shop = login(SHOP);

        assertThrows(ResponseStatusException.class, () -> controller.modify(rule(5, "SHOP2", ""), shop));
        assertThrows(ResponseStatusException.class, () -> controller.create(rule(null, "SHOP2", ""), shop));
        verify(service, never()).modify(any());
        verify(service, never()).create(any());

        controller.create(rule(null, " SHOP1 ", ""), shop);   // 앞뒤 공백은 잘라서 검사
        verify(service).create(any());
    }

    @Test
    void 채널_제한_계정은_사은품규칙을_다루지_못한다() {
        when(service.find(5)).thenReturn(rule(5, "SHOP1", ""));
        MockHttpServletRequest chnl = login(CHNL);

        assertThrows(ResponseStatusException.class, () -> controller.select(5, chnl));
        assertThrows(ResponseStatusException.class, () -> controller.create(rule(null, "SHOP1", ""), chnl));
        verify(service, never()).create(any());
    }

    @Test
    void 서비스_제한_계정은_자기_서비스의_상점과_채널만_고른다() {
        when(mapper.countShopInSvc("MINE", "SHP001")).thenReturn(1);
        when(mapper.countShopInSvc("FOREIGN", "SHP001")).thenReturn(0);
        when(mapper.countChnlInSvc("MYCH", "SHP001")).thenReturn(1);
        when(mapper.countChnlInSvc("FOREIGNCH", "SHP001")).thenReturn(0);
        MockHttpServletRequest svc = login(SVC);

        assertThrows(ResponseStatusException.class, () -> controller.create(rule(null, "FOREIGN", ""), svc));
        assertThrows(ResponseStatusException.class, () -> controller.create(rule(null, "MINE", "FOREIGNCH"), svc));
        verify(service, never()).create(any());

        controller.create(rule(null, "MINE", "MYCH"), svc);
        controller.create(rule(null, "MINE", ""), svc);   // 채널을 비우면 채널 검사 없음
        verify(service, org.mockito.Mockito.times(2)).create(any());
    }

    @Test
    void 없는_규칙은_가드를_통과해_서비스가_400으로_거절한다() {
        when(service.find(99)).thenReturn(null);

        controller.remove(99, login(SHOP));

        verify(service).remove(99);
    }
}
