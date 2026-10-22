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
        MockHttpServletRequest r = req("GET", "/order/orderprod/list", Map.of("svcAll", "Y", "loginId", "u", "shopCode", "MINE"));
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
        HttpServletRequest out = seen(on, req("GET", "/ship/ship-direct/list", Map.of("svcAll", "Y", "loginId", "u", "shopCode", "MINE")));
        assertEquals("MINE", out.getParameter("pShopCode"));
    }

    @Test
    void 채널_제한_계정은_pChnlCode_를_고정하고_상점은_건드리지_않는다() throws Exception {
        MockHttpServletRequest r = req("GET", "/order/orderprod/list", Map.of("svcAll", "Y", "loginId", "u", "chnlCode", "CH1"));
        r.setParameter("pShopCode", "ANY");
        HttpServletRequest out = seen(on, r);
        assertEquals("CH1", out.getParameter("pChnlCode"));
        assertEquals("ANY", out.getParameter("pShopCode"));   // 상점 제한이 없으니 좁히는 입력은 허용
    }

    @Test
    void 상점과_채널이_모두_있으면_둘_다_고정() throws Exception {
        HttpServletRequest out = seen(on, req("GET", "/adjust/adjust/list", Map.of("svcAll", "Y", "loginId", "u", "shopCode", "S", "chnlCode", "C")));
        assertEquals("S", out.getParameter("pShopCode"));
        assertEquals("C", out.getParameter("pChnlCode"));
    }

    @Test
    void 제한_없는_계정의_요청은_그대로_통과() throws Exception {
        MockHttpServletRequest r = req("GET", "/order/orderprod/list", Map.of("svcAll", "Y", "loginId", "admin", "shopCode", "", "chnlCode", ""));
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
        MockHttpServletRequest r = req("PUT", "/ship/ship-direct/invoice", Map.of("svcAll", "Y", "loginId", "u", "shopCode", "MINE"));
        assertSame(r, seen(on, r));
    }

    @Test
    void 공개_조회_경로도_관리자_세션이_있으면_같은_범위로_거르고_세션이_없는_고객은_건드리지_않는다() throws Exception {
        // 판매 상품 관리 화면이 스토어프론트 공개 조회 경로(/product/prod/list)를 같이 쓰므로 관리자 세션이 있으면 범위를 적용한다
        MockHttpServletRequest admin = req("GET", "/product/prod/list", Map.of("svcAll", "Y", "loginId", "u", "shopCode", "MINE"));
        assertEquals("MINE", seen(on, admin).getParameter("pShopCode"));
        // 관리자 세션이 없는 고객·비회원은 그대로
        MockHttpServletRequest customer = req("GET", "/product/prod/list", null);
        assertSame(customer, seen(on, customer));
    }

    @Test
    void 고객_상점_창고용_경로는_관리자_스코프를_적용하지_않는다() throws Exception {
        for (String uri : new String[]{"/order/orderprod/my/list", "/order/orderprod/shop/list", "/profile/cust/me", "/order/basket/list"}) {
            MockHttpServletRequest r = req("GET", uri, Map.of("svcAll", "Y", "loginId", "u", "shopCode", "MINE"));
            assertSame(r, seen(on, r), uri);
        }
    }

    @Test
    void 관리자_경로가_아니면_적용하지_않는다() throws Exception {
        MockHttpServletRequest r = req("GET", "/shop.html", Map.of("svcAll", "Y", "loginId", "u", "shopCode", "MINE"));
        assertSame(r, seen(on, r));
        assertNull(r.getParameter("pShopCode"));
    }

    @Test
    void 꺼져_있으면_적용하지_않는다() throws Exception {
        MockHttpServletRequest r = req("GET", "/order/orderprod/list", Map.of("svcAll", "Y", "loginId", "u", "shopCode", "MINE"));
        assertSame(r, seen(new DataScopeFilter(false), r));
    }

    @Test
    void 파라미터_이름_목록에도_고정_값이_포함된다() throws Exception {
        HttpServletRequest out = seen(on, req("GET", "/order/orderprod/list", Map.of("svcAll", "Y", "loginId", "u", "shopCode", "MINE")));
        assertTrue(java.util.Collections.list(out.getParameterNames()).contains("pShopCode"));
    }

    @Test
    void 서비스_제한_계정의_pSvcCode_는_위조해도_세션_서비스로_덮어쓴다() throws Exception {
        MockHttpServletRequest r = req("GET", "/order/orderprod/list", Map.of("loginId", "u", "svcCode", "SOH001", "svcAll", "N"));
        r.setParameter("pSvcCode", "SHP001");     // 샵피온 데이터를 보려는 위조
        HttpServletRequest out = seen(on, r);
        assertEquals("SOH001", out.getParameter("pSvcCode"));
    }

    @Test
    void pSvcCode_를_안_보내도_채워지고_svcCode_이름_조회도_좁힌다() throws Exception {
        HttpServletRequest out = seen(on, req("GET", "/product/prod/list", Map.of("loginId", "u", "svcCode", "ANY001", "svcAll", "N")));
        assertEquals("ANY001", out.getParameter("pSvcCode"));

        MockHttpServletRequest r = req("GET", "/catalog/cate/list", Map.of("loginId", "u", "svcCode", "ANY001", "svcAll", "N"));
        r.setParameter("svcCode", "SHP001");
        assertEquals("ANY001", seen(on, r).getParameter("svcCode"));
    }

    @Test
    void 전체_권한_계정은_pSvcCode_를_그대로_둔다() throws Exception {
        MockHttpServletRequest r = req("GET", "/order/orderprod/list", Map.of("loginId", "u", "svcCode", "SHP001", "svcAll", "Y"));
        r.setParameter("pSvcCode", "SOH001");
        assertSame(r, seen(on, r));                       // 래핑하지 않는다
        assertEquals("SOH001", r.getParameter("pSvcCode"));
    }

    @Test
    void svcAll_이_없는_기존_세션은_fail_closed_로_서비스_제한이_걸린다() throws Exception {
        HttpServletRequest out = seen(on, req("GET", "/order/orderprod/list", Map.of("loginId", "u", "svcCode", "SHP001")));
        assertEquals("SHP001", out.getParameter("pSvcCode"));
    }

    @Test
    void 서비스_제한은_쓰기_요청이나_보호_경로_밖은_건드리지_않는다() throws Exception {
        Map<String, String> soh = Map.of("loginId", "u", "svcCode", "SOH001", "svcAll", "N");
        MockHttpServletRequest post = req("POST", "/order/order/cancel-batch", soh);
        assertSame(post, seen(on, post));
        MockHttpServletRequest anon = req("GET", "/product/prod/list", null);   // 스토어프론트 고객·비회원(관리자 세션 없음)
        assertSame(anon, seen(on, anon));
    }
}
