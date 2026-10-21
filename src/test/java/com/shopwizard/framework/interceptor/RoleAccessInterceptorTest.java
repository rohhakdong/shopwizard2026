package com.shopwizard.framework.interceptor;

import com.shopwizard.authority.mapper.RolePermissionMapper;
import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.RolePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleAccessInterceptorTest {

    private static final String ROLE = "role-1";
    private RolePermissionMapper mapper;
    private RolePermissionService service;

    @BeforeEach
    void setUp() {
        mapper = mock(RolePermissionMapper.class);
        // 송장입력(쓰기) + 주문관리(조회)
        when(mapper.selectActiveMenus(ROLE)).thenReturn(List.of(
                Map.of("MenuCode", "m1", "MenuUri", "/wizardn/order/wOrdOrderProdList.wzn"),
                Map.of("MenuCode", "m2", "MenuUri", "/wizardn/ship/wShpShipInvoiceList.wzn")));
        when(mapper.selectActiveFunctns(ROLE)).thenReturn(List.of(
                Map.of("MenuCode", "m2", "FunctnUri", "/wizardn/ship/xShpShipInvoiceUpdate.wzn")));
        service = new RolePermissionService(mapper);
    }

    private RoleAccessInterceptor interceptor(String mode) {
        return new RoleAccessInterceptor(service, mode, "/product/prod", "/product/prod/list", "/company/shop/public");
    }

    private static MockHttpServletRequest asAdmin(String method, String uri, String roleUid) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        AuthSession.login(req, AuthSession.Type.MNGR, Map.of("loginId", "u1", "name", "홍", "roleUid", roleUid));
        return req;
    }

    private static boolean pass(RoleAccessInterceptor i, MockHttpServletRequest req, MockHttpServletResponse res) throws Exception {
        return i.preHandle(req, res, new Object());
    }

    @Test
    void 허용된_쓰기는_통과() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertTrue(pass(interceptor("enforce"), asAdmin("PUT", "/ship/ship-direct/invoice", ROLE), res));
        assertEquals(200, res.getStatus());
    }

    @Test
    void 권한_없는_쓰기는_403_과_메시지() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(pass(interceptor("enforce"), asAdmin("POST", "/order/order/refund-confirm-batch", ROLE), res));
        assertEquals(403, res.getStatus());
        assertTrue(res.getContentAsString().contains("권한"));
    }

    @Test
    void 권한_없는_조회도_403() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(pass(interceptor("enforce"), asAdmin("GET", "/profile/cust/list", ROLE), res));
        assertEquals(403, res.getStatus());
    }

    @Test
    void 역할이_없는_세션은_화면_API_를_쓸_수_없다() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(pass(interceptor("enforce"), asAdmin("GET", "/order/orderprod/list", ""), res));
        assertEquals(403, res.getStatus());
    }

    @Test
    void log_모드는_막지_않는다() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertTrue(pass(interceptor("log"), asAdmin("POST", "/order/order/refund-confirm-batch", ROLE), res));
        assertEquals(200, res.getStatus());
    }

    @Test
    void off_모드는_검사하지_않는다() throws Exception {
        assertTrue(pass(interceptor("off"), asAdmin("POST", "/order/order/refund-confirm-batch", ROLE), new MockHttpServletResponse()));
        verify(mapper, times(0)).selectActiveMenus(ROLE);
    }

    @Test
    void 스토어프론트_공개_조회는_관리자_세션이_있어도_역할과_무관() throws Exception {
        assertTrue(pass(interceptor("enforce"), asAdmin("GET", "/product/prod/list", "no-role-at-all"), new MockHttpServletResponse()));
        // 같은 경로의 쓰기는 검사 대상
        assertFalse(pass(interceptor("enforce"), asAdmin("POST", "/product/prod", "no-role-at-all"), new MockHttpServletResponse()));
    }

    @Test
    void 관리자_로그인이_아니면_이_인터셉터는_관여하지_않는다() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/order/orderprod/list");
        assertTrue(pass(interceptor("enforce"), req, new MockHttpServletResponse()));   // 401 은 AuthInterceptor 의 몫
    }

    @Test
    void 권한은_캐시되고_invalidate_로_다시_읽는다() throws Exception {
        RoleAccessInterceptor i = interceptor("enforce");
        pass(i, asAdmin("GET", "/order/orderprod/list", ROLE), new MockHttpServletResponse());
        pass(i, asAdmin("GET", "/order/orderprod/list", ROLE), new MockHttpServletResponse());
        verify(mapper, times(1)).selectActiveMenus(ROLE);
        service.invalidate();
        pass(i, asAdmin("GET", "/order/orderprod/list", ROLE), new MockHttpServletResponse());
        verify(mapper, times(2)).selectActiveMenus(ROLE);
    }
}
