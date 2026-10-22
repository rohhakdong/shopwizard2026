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

    @Test
    void 서비스는_역할의_서비스로_제한하고_전체_권한_계정만_제한이_없다() {
        DataScope soho = DataScope.of(asAdmin(Map.of("loginId", "u", "svcCode", "SOH001", "svcAll", "N")));
        assertEquals("SOH001", soho.svcCode());
        assertTrue(soho.svcScoped());
        assertFalse(soho.scoped());          // 상점/채널 제한 의미는 그대로 — 정산 쓰기(requireUnscoped)가 서비스 때문에 막히면 안 된다
        assertTrue(soho.anyScoped());

        DataScope all = DataScope.of(asAdmin(Map.of("loginId", "u", "svcCode", "SHP001", "svcAll", "Y")));
        assertNull(all.svcCode());
        assertFalse(all.svcScoped());
        assertFalse(all.anyScoped());
    }

    @Test
    void svcAll_claim_이_없는_기존_세션이나_서비스가_없는_역할은_fail_closed() {
        DataScope old = DataScope.of(asAdmin(Map.of("loginId", "u", "svcCode", "SHP001")));   // svcAll 없음
        assertEquals("SHP001", old.svcCode());

        DataScope noSvc = DataScope.of(asAdmin(Map.of("loginId", "u", "svcCode", " ", "svcAll", "N")));
        assertEquals(DataScope.NO_SERVICE, noSvc.svcCode());   // 어떤 서비스와도 일치하지 않는다
        assertTrue(noSvc.svcScoped());
    }
}
