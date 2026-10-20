package com.shopwizard.framework.auth;

import com.shopwizard.framework.auth.AuthSession.Type;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthSessionTest {

    @Test
    void 로그인하면_claims를_서버세션에_저장하고_null은_빈문자열이_된다() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        Map<String, String> claims = new HashMap<>();
        claims.put("loginId", "admin");
        claims.put("chnlCode", null);

        AuthSession.login(req, Type.MNGR, claims);

        Map<String, String> stored = AuthSession.get(req, Type.MNGR);
        assertEquals("admin", stored.get("loginId"));
        assertEquals("", stored.get("chnlCode"));
        assertTrue(AuthSession.isLoggedIn(req, Type.MNGR));
        assertFalse(AuthSession.isLoggedIn(req, Type.CUST));
    }

    @Test
    void 쿠키_값은_인증에_쓰이지_않는다() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new jakarta.servlet.http.Cookie("cust_id", "2"));

        assertNull(AuthSession.get(req, Type.CUST));
        assertNull(AuthSession.custId(req));
    }

    @Test
    void 기존_세션이_있으면_로그인할_때_세션ID를_새로_발급한다() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpSession before = new MockHttpSession();
        req.setSession(before);
        String oldId = before.getId();

        AuthSession.login(req, Type.CUST, Map.of("custId", "7"));

        assertNotEquals(oldId, req.getSession().getId(), "세션 고정 공격 방지: 로그인 시 세션 ID 교체");
        assertEquals(Integer.valueOf(7), AuthSession.custId(req));
    }

    @Test
    void 로그아웃은_해당_주체만_해제하고_모두_없어지면_세션을_폐기한다() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, Type.MNGR, Map.of("loginId", "admin"));
        AuthSession.login(req, Type.CUST, Map.of("custId", "2"));
        MockHttpSession session = (MockHttpSession) req.getSession();

        AuthSession.logout(req, Type.CUST);
        assertFalse(AuthSession.isLoggedIn(req, Type.CUST));
        assertTrue(AuthSession.isLoggedIn(req, Type.MNGR));
        assertFalse(session.isInvalid());

        AuthSession.logout(req, Type.MNGR);
        assertTrue(session.isInvalid());
    }

    @Test
    void custId는_숫자가_아니면_null() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, Type.CUST, Map.of("custId", "abc"));

        assertNull(AuthSession.custId(req));
    }

    @Test
    void 세션이_없으면_로그아웃해도_예외가_없다() {
        AuthSession.logout(new MockHttpServletRequest(), Type.MNGR);
    }
}
