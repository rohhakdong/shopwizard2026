package com.shopwizard.order.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import com.shopwizard.order.model.OrderKeywrd;
import com.shopwizard.order.service.OrderKeywrdService;
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

/** 키워드 규칙의 데이터 범위 — 상점 제한은 자기 상점, 채널 제한은 불가, 서비스 제한은 자기 서비스 상점. */
class OrderKeywrdControllerTest {

    private DataScopeMapper mapper;
    private OrderKeywrdService service;
    private OrderKeywrdController controller;

    @BeforeEach
    void setUp() {
        mapper = mock(DataScopeMapper.class);
        DataScopeGuard scopeGuard = new DataScopeGuard(mapper);
        ReflectionTestUtils.setField(scopeGuard, "mode", "enforce");
        service = mock(OrderKeywrdService.class);
        controller = new OrderKeywrdController(service, new OrderMasterGuard(scopeGuard));
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

    private static OrderKeywrd kw(Integer seq, String shop) {
        OrderKeywrd k = new OrderKeywrd();
        k.setKeywrdSeq(seq);
        k.setShopCode(shop);
        return k;
    }

    @Test
    void 제한_없는_계정은_그대로_서비스로_간다() {
        OrderKeywrd body = kw(null, "ANY");
        controller.create(body, login(ALL));
        verify(service).create(body);
    }

    @Test
    void 상점_제한_계정은_남의_상점_키워드를_조회_수정_삭제_등록할_수_없다() {
        when(service.find(5)).thenReturn(kw(5, "SHOP2"));
        MockHttpServletRequest shop = login(SHOP);

        assertThrows(ResponseStatusException.class, () -> controller.select(5, shop));
        assertThrows(ResponseStatusException.class, () -> controller.remove(5, shop));
        assertThrows(ResponseStatusException.class, () -> controller.modify(kw(5, "SHOP1"), shop));
        assertThrows(ResponseStatusException.class, () -> controller.create(kw(null, "SHOP2"), shop));
        verify(service, never()).remove(any());
        verify(service, never()).modify(any());
        verify(service, never()).create(any());

        controller.create(kw(null, " SHOP1 "), shop);
        verify(service).create(any());
    }

    @Test
    void 채널_제한_계정은_키워드를_다루지_못하고_서비스_제한은_자기_서비스_상점만() {
        assertThrows(ResponseStatusException.class, () -> controller.create(kw(null, "SHOP1"), login(CHNL)));

        when(mapper.countShopInSvc("MINE", "SHP001")).thenReturn(1);
        when(mapper.countShopInSvc("FOREIGN", "SHP001")).thenReturn(0);
        MockHttpServletRequest svc = login(SVC);
        controller.create(kw(null, "MINE"), svc);
        assertThrows(ResponseStatusException.class, () -> controller.create(kw(null, "FOREIGN"), svc));
        verify(service, org.mockito.Mockito.times(1)).create(any());
    }
}
