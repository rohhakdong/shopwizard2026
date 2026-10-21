package com.shopwizard.framework.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataScopeFilterTest {

    private static MockHttpServletRequest req(String method, String uri, Map<String, String> claims) {
        MockHttpServletRequest r = new MockHttpServletRequest(method, uri);
        r.setRequestURI(uri);
        if (claims != null) AuthSession.login(r, AuthSession.Type.MNGR, claims);
        return r;
    }

    /** 필터를 통과해 체인이 실제로 받은 요청. */
    private static HttpServletRequest seen(DataScopeFilter filter, MockHttpServletRequest r) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(r, new MockHttpServletResponse(), chain);
        return (HttpServletRequest) chain.getRequest();
    }

    private final DataScopeFilter on = new DataScopeFilter(true);

    @Test
    void 상점_제한_계정의_조회는_pShopCode_를_세션_값으로_덮어쓴다() throws Exception {
        MockHttpServletRequest r = req("GET", "/order/orderprod/list", Map.of("loginId", "u", "shopCode", "MINE"));
        r.setParameter("pShopCode", "OTHER");     // 다른 업체 코드를 보내도
        r.setParameter("pStartDate", "2026-01-01");

        HttpServletRequest out = seen(on, r);
        assertEquals("MINE", out.getParameter("pShopCode"));
        assertArrayEquals(new String[]{"MINE"}, out.getParameterValues("pShopCode"));
        assertEquals("MINE", out.getParameterMap().get("pShopCode")[0]);
        assertEquals("2026-01-01", out.getParameter("pStartDate"));   // 나머지 파라미터는 그대로
    }

    @Test
    void 클라이언트가_pShopCode_를_안_보내도_채워진다() throws Exception {
        HttpServletRequest out = seen(on, req("GET", "/ship/ship-direct/list", Map.of("loginId", "u", "shopCode", "MINE")));
        assertEquals("MINE", out.getParameter("pShopCode"));
    }

    @Test
    void 채널_제한_계정은_pChnlCode_를_고정하고_상점은_건드리지_않는다() throws Exception {
        MockHttpServletRequest r = req("GET", "/order/orderprod/list", Map.of("loginId", "u", "chnlCode", "CH1"));
        r.setParameter("pShopCode", "ANY");
        HttpServletRequest out = seen(on, r);
        assertEquals("CH1", out.getParameter("pChnlCode"));
        assertEquals("ANY", out.getParameter("pShopCode"));   // 상점 제한이 없으니 좁히는 입력은 허용
    }

    @Test
    void 상점과_채널이_모두_있으면_둘_다_고정() throws Exception {
        HttpServletRequest out = seen(on, req("GET", "/adjust/adjust/list", Map.of("loginId", "u", "shopCode", "S", "chnlCode", "C")));
        assertEquals("S", out.getParameter("pShopCode"));
        assertEquals("C", out.getParameter("pChnlCode"));
    }

    @Test
    void 제한_없는_계정의_요청은_그대로_통과() throws Exception {
        MockHttpServletRequest r = req("GET", "/order/orderprod/list", Map.of("loginId", "admin", "shopCode", "", "chnlCode", ""));
        r.setParameter("pShopCode", "X");
        assertSame(r, seen(on, r));
    }

    @Test
    void 로그인하지_않은_요청도_그대로_통과_인증은_인터셉터의_몫() throws Exception {
        MockHttpServletRequest r = req("GET", "/order/orderprod/list", null);
        assertSame(r, seen(on, r));
    }

    @Test
    void 쓰기_요청은_건드리지_않는다_가드가_소유권으로_막는다() throws Exception {
        MockHttpServletRequest r = req("PUT", "/ship/ship-direct/invoice", Map.of("loginId", "u", "shopCode", "MINE"));
        assertSame(r, seen(on, r));
    }

    @Test
    void 스토어프론트_공개_조회는_관리자_세션이_있어도_건드리지_않는다() throws Exception {
        MockHttpServletRequest r = req("GET", "/product/prod/list", Map.of("loginId", "u", "shopCode", "MINE"));
        assertSame(r, seen(on, r));
    }

    @Test
    void 고객_상점_창고용_경로는_관리자_스코프를_적용하지_않는다() throws Exception {
        for (String uri : new String[]{"/order/orderprod/my/list", "/order/orderprod/shop/list", "/profile/cust/me", "/order/basket/list"}) {
            MockHttpServletRequest r = req("GET", uri, Map.of("loginId", "u", "shopCode", "MINE"));
            assertSame(r, seen(on, r), uri);
        }
    }

    @Test
    void 관리자_경로가_아니면_적용하지_않는다() throws Exception {
        MockHttpServletRequest r = req("GET", "/shop.html", Map.of("loginId", "u", "shopCode", "MINE"));
        assertSame(r, seen(on, r));
        assertNull(r.getParameter("pShopCode"));
    }

    @Test
    void 꺼져_있으면_적용하지_않는다() throws Exception {
        MockHttpServletRequest r = req("GET", "/order/orderprod/list", Map.of("loginId", "u", "shopCode", "MINE"));
        assertSame(r, seen(new DataScopeFilter(false), r));
    }

    @Test
    void 파라미터_이름_목록에도_고정_값이_포함된다() throws Exception {
        HttpServletRequest out = seen(on, req("GET", "/order/orderprod/list", Map.of("loginId", "u", "shopCode", "MINE")));
        assertTrue(java.util.Collections.list(out.getParameterNames()).contains("pShopCode"));
    }
}
