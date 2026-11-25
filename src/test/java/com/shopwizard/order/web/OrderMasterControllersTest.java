package com.shopwizard.order.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import com.shopwizard.order.model.OrderProdCode;
import com.shopwizard.order.model.OrderUprice;
import com.shopwizard.order.model.ProdCodeMatch;
import com.shopwizard.order.service.OrderProdCodeService;
import com.shopwizard.order.service.OrderUpriceService;
import com.shopwizard.order.service.ProdCodeMatchService;
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

/** 발주 마스터 화면(발주상품코드·발주단가표·상품매칭결과)의 데이터 범위 — 상점·채널·서비스 제한 계정과 공통(상점 없음) 행. */
class OrderMasterControllersTest {

    private DataScopeMapper mapper;
    private OrderMasterGuard guard;
    private OrderProdCodeService prodCodeService;
    private OrderUpriceService upriceService;
    private ProdCodeMatchService matchService;
    private OrderProdCodeController prodCode;
    private OrderUpriceController uprice;
    private ProdCodeMatchController match;

    @BeforeEach
    void setUp() {
        mapper = mock(DataScopeMapper.class);
        DataScopeGuard scopeGuard = new DataScopeGuard(mapper);
        ReflectionTestUtils.setField(scopeGuard, "mode", "enforce");
        guard = new OrderMasterGuard(scopeGuard);
        prodCodeService = mock(OrderProdCodeService.class);
        upriceService = mock(OrderUpriceService.class);
        matchService = mock(ProdCodeMatchService.class);
        prodCode = new OrderProdCodeController(prodCodeService, guard);
        uprice = new OrderUpriceController(upriceService, guard);
        match = new ProdCodeMatchController(matchService, scopeGuard);
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

    private static OrderUprice uprice(String code, String shop) {
        OrderUprice u = new OrderUprice();
        u.setMatchUpriceCode(code);
        u.setShopCode(shop);
        return u;
    }

    private static OrderProdCode prodCode(String code, String shop) {
        OrderProdCode c = new OrderProdCode();
        c.setMatchProdCode(code);
        c.setShopCode(shop);
        return c;
    }

    // ── OrderMasterGuard ──

    @Test
    void 제한_없는_계정은_공통_행도_상점_행도_다룬다() {
        guard.requireShop(login(ALL), "");
        guard.requireShop(login(ALL), null);
        guard.requireShop(login(ALL), "ANY");
    }

    @Test
    void 상점_제한_계정은_자기_상점_행만_공통_행은_못_다룬다() {
        MockHttpServletRequest shop = login(SHOP);
        guard.requireShop(shop, "SHOP1");
        assertThrows(ResponseStatusException.class, () -> guard.requireShop(shop, "SHOP2"));
        assertThrows(ResponseStatusException.class, () -> guard.requireShop(shop, ""));
    }

    @Test
    void 채널_제한_계정은_마스터를_다루지_못한다() {
        MockHttpServletRequest chnl = login(CHNL);
        assertThrows(ResponseStatusException.class, () -> guard.requireShop(chnl, "SHOP1"));
        assertThrows(ResponseStatusException.class, () -> guard.requireShop(chnl, null));
    }

    @Test
    void 서비스_제한_계정은_자기_서비스_상점과_공통_행만() {
        when(mapper.countShopInSvc("MINE", "SHP001")).thenReturn(1);
        when(mapper.countShopInSvc("FOREIGN", "SHP001")).thenReturn(0);
        MockHttpServletRequest svc = login(SVC);

        guard.requireShop(svc, "MINE");
        guard.requireShop(svc, "");
        assertThrows(ResponseStatusException.class, () -> guard.requireShop(svc, "FOREIGN"));
    }

    // ── 발주단가표 ──

    @Test
    void 발주단가표_남의_상점_코드는_조회_수정_삭제가_모두_403이고_서비스를_부르지_않는다() {
        when(upriceService.find("X-1")).thenReturn(uprice("X-1", "SHOP2"));
        MockHttpServletRequest shop = login(SHOP);

        assertThrows(ResponseStatusException.class, () -> uprice.select("X-1", shop));
        assertThrows(ResponseStatusException.class, () -> uprice.remove("X-1", shop));
        OrderUprice body = uprice("X-1", "SHOP1");
        assertThrows(ResponseStatusException.class, () -> uprice.modify(body, shop));
        verify(upriceService, never()).remove(any());
        verify(upriceService, never()).modify(any());
    }

    @Test
    void 발주단가표_상점_제한_계정은_다른_상점으로_옮기거나_다른_상점에_등록할_수_없다() {
        when(upriceService.find("X-1")).thenReturn(uprice("X-1", "SHOP1"));
        MockHttpServletRequest shop = login(SHOP);

        assertThrows(ResponseStatusException.class, () -> uprice.modify(uprice("X-1", "SHOP2"), shop));
        assertThrows(ResponseStatusException.class, () -> uprice.create(uprice(null, "SHOP2"), shop));
        verify(upriceService, never()).modify(any());
        verify(upriceService, never()).create(any());

        uprice.create(uprice(null, " SHOP1 "), shop);   // 앞뒤 공백은 잘라서 검사
        verify(upriceService).create(any());
    }

    @Test
    void 발주단가표_공통_코드는_제한_없는_계정만_지운다() {
        when(upriceService.find("COMMON-1")).thenReturn(uprice("COMMON-1", ""));

        assertThrows(ResponseStatusException.class, () -> uprice.remove("COMMON-1", login(SHOP)));
        uprice.remove("COMMON-1", login(ALL));
        verify(upriceService).remove("COMMON-1");
    }

    @Test
    void 발주단가표_없는_코드는_가드를_통과해_서비스가_400으로_거절한다() {
        when(upriceService.find("NONE")).thenReturn(null);

        uprice.remove("NONE", login(SHOP));

        verify(upriceService).remove("NONE");
    }

    // ── 발주상품코드 ──

    @Test
    void 발주상품코드_남의_상점_행은_수정_삭제_단건_조회가_403() {
        when(prodCodeService.find("K::1x1")).thenReturn(prodCode("K::1x1", "SHOP2"));
        MockHttpServletRequest shop = login(SHOP);

        assertThrows(ResponseStatusException.class, () -> prodCode.select("K::1x1", shop));
        assertThrows(ResponseStatusException.class, () -> prodCode.remove("K::1x1", shop));
        assertThrows(ResponseStatusException.class, () -> prodCode.modify(prodCode("K::1x1", "SHOP1"), shop));
        verify(prodCodeService, never()).remove(any());
        verify(prodCodeService, never()).modify(any());
    }

    @Test
    void 발주상품코드_상점_제한_계정은_자기_상점에만_등록한다() {
        MockHttpServletRequest shop = login(SHOP);

        assertThrows(ResponseStatusException.class, () -> prodCode.create(prodCode(null, "SHOP2"), shop));
        verify(prodCodeService, never()).create(any());

        prodCode.create(prodCode(null, "SHOP1"), shop);
        verify(prodCodeService).create(any());
    }

    @Test
    void 발주상품코드_상점이_빈_등록은_가드를_건너뛰고_서비스가_400으로_거절한다() {
        prodCode.create(prodCode(null, " "), login(SHOP));

        verify(prodCodeService).create(any());
    }

    // ── 상품매칭결과 ──

    private static ProdCodeMatch rule(String name, String option, String prod) {
        ProdCodeMatch m = new ProdCodeMatch();
        m.setProdName(name);
        m.setProdOption(option);
        m.setProdCode(prod);
        return m;
    }

    @Test
    void 상품매칭결과_상점_제한_계정은_자기_상품_규칙만_다룬다() {
        when(mapper.countPrdProd("MINE", "SHOP1", null)).thenReturn(1);
        when(mapper.countPrdProd("OTHER", "SHOP1", null)).thenReturn(0);
        when(matchService.find("상품", "옵션")).thenReturn(rule("상품", "옵션", "OTHER"));
        MockHttpServletRequest shop = login(SHOP);

        assertThrows(ResponseStatusException.class, () -> match.select("상품", "옵션", shop));
        assertThrows(ResponseStatusException.class, () -> match.remove("상품", "옵션", shop));
        assertThrows(ResponseStatusException.class, () -> match.modify(rule("상품", "옵션", "MINE"), shop));   // 남의 규칙을 내 상품으로 가로채기
        assertThrows(ResponseStatusException.class, () -> match.create(rule("새상품", "", "OTHER"), shop));
        verify(matchService, never()).remove(any(), any());
        verify(matchService, never()).modify(any());
        verify(matchService, never()).create(any());

        match.create(rule("새상품", "", "MINE"), shop);
        verify(matchService).create(any());
    }

    @Test
    void 상품매칭결과_제한_없는_계정은_가드_없이_통과하고_상품코드가_빈_수정은_제한_계정에서_403() {
        match.create(rule("상품", "", "ANY"), login(ALL));
        verify(matchService).create(any());

        assertThrows(ResponseStatusException.class, () -> match.create(rule("상품", "", " "), login(SHOP)));   // 코드가 없으면 소유를 확인할 수 없다
        verify(matchService, org.mockito.Mockito.times(1)).create(any());   // 위의 제한 없는 등록 한 번뿐
    }
}
