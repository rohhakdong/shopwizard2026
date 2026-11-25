package com.shopwizard.ship.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import com.shopwizard.order.web.OrderMasterGuard;
import com.shopwizard.ship.model.ShipPlaceRule;
import com.shopwizard.ship.service.DeliRstrctZipcodeService;
import com.shopwizard.ship.service.ShipPlaceRuleService;
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

/** 배송제한 규칙은 상점 단위 데이터 범위, 우편번호 표 쓰기는 제한 없는 계정만. */
class ShipPlaceRuleControllerTest {

    private DataScopeMapper scopeMapper;
    private ShipPlaceRuleService service;
    private DeliRstrctZipcodeService zipService;
    private ShipPlaceRuleController controller;
    private DeliRstrctZipcodeController zipController;

    @BeforeEach
    void setUp() {
        scopeMapper = mock(DataScopeMapper.class);
        DataScopeGuard scopeGuard = new DataScopeGuard(scopeMapper);
        ReflectionTestUtils.setField(scopeGuard, "mode", "enforce");
        service = mock(ShipPlaceRuleService.class);
        zipService = mock(DeliRstrctZipcodeService.class);
        controller = new ShipPlaceRuleController(service, zipService, new OrderMasterGuard(scopeGuard));
        zipController = new DeliRstrctZipcodeController(zipService, scopeGuard);
    }

    private static MockHttpServletRequest login(Map<String, String> claims) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, claims);
        AuthSession.markActor(req, AuthSession.Type.MNGR);
        return req;
    }

    private static final Map<String, String> ALL = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자");
    private static final Map<String, String> SHOP = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자", "shopCode", "SHOP1");
    private static final Map<String, String> CHNL = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자", "chnlCode", "CH1");
    private static final Map<String, String> SVC = Map.of("loginId", "u", "name", "사용자", "svcCode", "SHP001");

    private static ShipPlaceRule rule(Integer no, String shop) {
        ShipPlaceRule r = new ShipPlaceRule();
        r.setRuleNo(no);
        r.setShopCode(shop);
        return r;
    }

    @Test
    void 상점_제한_계정은_남의_상점_규칙을_조회_수정_삭제_등록할_수_없다() {
        when(service.find(5)).thenReturn(rule(5, "SHOP2"));
        MockHttpServletRequest shop = login(SHOP);

        assertThrows(ResponseStatusException.class, () -> controller.select(5, shop));
        assertThrows(ResponseStatusException.class, () -> controller.remove(5, shop));
        assertThrows(ResponseStatusException.class, () -> controller.modify(rule(5, "SHOP1"), shop));
        assertThrows(ResponseStatusException.class, () -> controller.create(rule(null, "SHOP2"), shop));
        verify(service, never()).create(any());
        verify(service, never()).modify(any());
        verify(service, never()).remove(any());

        controller.create(rule(null, " SHOP1 "), shop);
        verify(service).create(any());
    }

    @Test
    void 채널_제한_계정은_다루지_못하고_서비스_제한은_자기_서비스_상점만() {
        assertThrows(ResponseStatusException.class, () -> controller.create(rule(null, "SHOP1"), login(CHNL)));

        when(scopeMapper.countShopInSvc("MINE", "SHP001")).thenReturn(1);
        when(scopeMapper.countShopInSvc("FOREIGN", "SHP001")).thenReturn(0);
        MockHttpServletRequest svc = login(SVC);
        controller.create(rule(null, "MINE"), svc);
        assertThrows(ResponseStatusException.class, () -> controller.create(rule(null, "FOREIGN"), svc));
        verify(service, org.mockito.Mockito.times(1)).create(any());
    }

    @Test
    void 우편번호_표의_등록_삭제는_제한_없는_계정만_할_수_있다() {
        zipController.bulk(Map.of("sect", "S", "zipcodes", "06236"), login(ALL));
        zipController.delete("S", "06236", login(ALL));
        zipController.delete("S", null, login(ALL));
        verify(zipService).bulkAdd("S", "06236", "u", "사용자");
        verify(zipService).remove("S", "06236");
        verify(zipService).removeSect("S");

        for (Map<String, String> restricted : java.util.List.of(SHOP, CHNL, SVC)) {
            MockHttpServletRequest req = login(restricted);
            assertThrows(ResponseStatusException.class, () -> zipController.bulk(Map.of("sect", "S", "zipcodes", "06236"), req));
            assertThrows(ResponseStatusException.class, () -> zipController.delete("S", "06236", req));
        }
        verify(zipService, org.mockito.Mockito.times(1)).bulkAdd(any(), any(), any(), any());
    }
}
