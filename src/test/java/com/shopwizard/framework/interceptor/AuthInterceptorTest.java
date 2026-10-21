package com.shopwizard.framework.interceptor;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.AuthSession.Type;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthInterceptorTest {

    private final AuthInterceptor mngr = new AuthInterceptor(Type.MNGR,
            "/product/prod", "/product/prod/list", "/company/shop/public");
    private final AuthInterceptor shop = new AuthInterceptor(Type.SHOP);
    private final AuthInterceptor cust = new AuthInterceptor(Type.CUST);

    private static MockHttpServletRequest request(String method, String uri) {
        return new MockHttpServletRequest(method, uri);
    }

    private static boolean pass(AuthInterceptor interceptor, MockHttpServletRequest req, MockHttpServletResponse res) throws Exception {
        return interceptor.preHandle(req, res, new Object());
    }

    @Test
    void 쿠키만_위조하면_401() throws Exception {
        MockHttpServletRequest req = request("GET", "/order/orderprod/list");
        req.setCookies(
                new Cookie("mngr_loginId", "hacker"),
                new Cookie("cust_id", "2"),
                new Cookie("shop_code", "ANY"),
                new Cookie("warehs_code", "ANY"));
        MockHttpServletResponse res = new MockHttpServletResponse();

        assertFalse(pass(mngr, req, res));
        assertEquals(401, res.getStatus());
        assertFalse(pass(cust, req, new MockHttpServletResponse()));
        assertFalse(pass(shop, req, new MockHttpServletResponse()));
    }

    @Test
    void 세션에_로그인이_있어야_통과한다() throws Exception {
        MockHttpServletRequest req = request("GET", "/order/orderprod/list");
        AuthSession.login(req, Type.MNGR, Map.of("loginId", "admin"));

        assertTrue(pass(mngr, req, new MockHttpServletResponse()));
    }

    @Test
    void 주체_간_로그인은_서로_격리된다() throws Exception {
        MockHttpServletRequest req = request("GET", "/company/shop/me");
        AuthSession.login(req, Type.MNGR, Map.of("loginId", "admin"));

        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(pass(shop, req, res));
        assertEquals(401, res.getStatus());
        assertFalse(pass(cust, req, new MockHttpServletResponse()));

        AuthSession.login(req, Type.CUST, Map.of("custId", "2"));
        assertTrue(pass(cust, req, new MockHttpServletResponse()));
        assertTrue(pass(mngr, req, new MockHttpServletResponse()));
        assertFalse(pass(shop, req, new MockHttpServletResponse()));
    }

    @Test
    void 공개조회_경로는_GET만_로그인_없이_통과한다() throws Exception {
        assertTrue(pass(mngr, request("GET", "/product/prod"), new MockHttpServletResponse()));
        assertTrue(pass(mngr, request("HEAD", "/product/prod/list"), new MockHttpServletResponse()));

        for (String method : new String[]{"POST", "PUT", "DELETE", "PATCH"}) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            assertFalse(pass(mngr, request(method, "/product/prod"), res), method + " /product/prod 는 막혀야 한다");
            assertEquals(401, res.getStatus());
        }
    }

    @Test
    void 공개목록에_없는_경로는_GET도_막는다() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(pass(mngr, request("GET", "/order/order"), res));
        assertEquals(401, res.getStatus());
        assertFalse(pass(mngr, request("GET", "/authority/mngr/list"), new MockHttpServletResponse()));
        // 하위 경로는 공개 패턴(정확 일치)에 포함되지 않는다
        assertFalse(pass(mngr, request("GET", "/product/prod/supplyqty"), new MockHttpServletResponse()));
    }

    @Test
    void 세션_인증에_성공하면_로그인_정보로_행위자를_표시한다() throws Exception {
        MockHttpServletRequest req = request("POST", "/catalog/brand");
        AuthSession.login(req, Type.MNGR, Map.of("loginId", "admin01", "name", "홍길동"));

        assertTrue(pass(mngr, req, new MockHttpServletResponse()));

        AuthSession.Actor actor = AuthSession.actor(req);
        assertEquals("admin01", actor.loginId());
        assertEquals("홍길동", actor.name());
    }

    @Test
    void 행위자는_인증을_통과시킨_주체의_값이다() throws Exception {
        MockHttpServletRequest req = request("POST", "/order/basket");
        AuthSession.login(req, Type.MNGR, Map.of("loginId", "admin01", "name", "관리자"));
        AuthSession.login(req, Type.CUST, Map.of("loginId", "cust01", "name", "고객", "custId", "2"));

        assertTrue(pass(cust, req, new MockHttpServletResponse()));

        assertEquals("cust01", AuthSession.actor(req).loginId());   // 관리자 세션이 같이 있어도 고객 경로에선 고객
    }

    @Test
    void 공개조회로_통과했거나_인증에_실패한_요청에는_행위자를_표시하지_않는다() throws Exception {
        MockHttpServletRequest publicRead = request("GET", "/product/prod");
        AuthSession.login(publicRead, Type.MNGR, Map.of("loginId", "admin01"));
        assertTrue(pass(mngr, publicRead, new MockHttpServletResponse()));
        assertEquals(null, AuthSession.actor(publicRead));

        MockHttpServletRequest anonymous = request("POST", "/catalog/brand");
        assertFalse(pass(mngr, anonymous, new MockHttpServletResponse()));
        assertEquals(null, AuthSession.actor(anonymous));
    }

    @Test
    void OPTIONS_사전요청은_통과한다() throws Exception {
        assertTrue(pass(mngr, request("OPTIONS", "/order/order"), new MockHttpServletResponse()));
    }
}
