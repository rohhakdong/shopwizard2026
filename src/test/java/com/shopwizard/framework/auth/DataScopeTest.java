package com.shopwizard.framework.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataScopeTest {

    private static MockHttpServletRequest asAdmin(Map<String, String> claims) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/order/orderprod/list");
        AuthSession.login(req, AuthSession.Type.MNGR, claims);
        return req;
    }

    @Test
    void 세션의_상점코드가_있으면_상점으로_제한된다() {
        DataScope s = DataScope.of(asAdmin(Map.of("loginId", "u", "shopCode", "SHOP1", "chnlCode", "")));
        assertTrue(s.scoped());
        assertEquals("SHOP1", s.shopCode());
        assertNull(s.chnlCode());
    }

    @Test
    void 채널코드만_있으면_채널로_제한된다() {
        DataScope s = DataScope.of(asAdmin(Map.of("loginId", "u", "chnlCode", "CH1")));
        assertTrue(s.scoped());
        assertNull(s.shopCode());
        assertEquals("CH1", s.chnlCode());
    }

    @Test
    void 둘_다_있으면_둘_다_담는다_AND_조건에_쓰인다() {
        DataScope s = DataScope.of(asAdmin(Map.of("loginId", "u", "shopCode", "SHOP1", "chnlCode", "CH1")));
        assertEquals("SHOP1", s.shopCode());
        assertEquals("CH1", s.chnlCode());
    }

    @Test
    void 코드가_비어_있거나_공백이면_제한_없음() {
        assertFalse(DataScope.of(asAdmin(Map.of("loginId", "u", "shopCode", "", "chnlCode", "  "))).scoped());
        assertFalse(DataScope.of(asAdmin(Map.of("loginId", "u"))).scoped());
    }

    @Test
    void 관리자_로그인이_아니면_제한_없음_인증은_다른_계층의_몫() {
        assertFalse(DataScope.of(new MockHttpServletRequest()).scoped());
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.SHOP, Map.of("shopCode", "S1"));   // 상점 계정 세션은 MNGR 스코프가 아님
        assertFalse(DataScope.of(req).scoped());
    }
}
