package com.shopwizard.ship.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.MngrPaths;
import com.shopwizard.ship.mapper.ShipCheckMapper;
import com.shopwizard.ship.model.ShipCheck;
import com.shopwizard.ship.service.ShipAutoOrderService;
import com.shopwizard.ship.service.ShipCheckService;
import com.shopwizard.ship.service.VendorShipCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 업체(상점·창고) 발주 목록·발주서 — 허용 상점 집합 안에서만 보고 받고, 번호 필수, 남의 번호는 없는 번호와 똑같이 404, 출력 횟수 불변. */
class VendorShipCheckControllerTest {

    private ShipCheckMapper mapper;
    private ShipCheckService checkService;
    private ShipAutoOrderService autoOrderService;
    private VendorShipCheckController controller;

    @BeforeEach
    void setUp() {
        mapper = mock(ShipCheckMapper.class);
        checkService = mock(ShipCheckService.class);
        autoOrderService = mock(ShipAutoOrderService.class);
        when(autoOrderService.download(any(), any(), any(), any(), eq(false))).thenReturn(new ShipAutoOrderService.Download("a.xls", new byte[]{1}));
        controller = new VendorShipCheckController(new VendorShipCheckService(mapper, checkService, autoOrderService));
    }

    private static MockHttpServletRequest shop(String shopCode) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.SHOP, Map.of("shopCode", shopCode, "loginId", "shopUser", "name", "업체"));
        AuthSession.markActor(req, AuthSession.Type.SHOP);
        return req;
    }

    private static MockHttpServletRequest warehs(String warehsCode) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.WAREHS, Map.of("warehsCode", warehsCode, "loginId", "whUser", "name", "창고"));
        AuthSession.markActor(req, AuthSession.Type.WAREHS);
        return req;
    }

    private static ShipCheck header(int no, String shop) {
        ShipCheck h = new ShipCheck();
        h.setShipCheckNo(no);
        h.setShopCode(shop);
        return h;
    }

    @Test
    void 상점은_자기_상점의_발주_목록만_조회한다() {
        when(mapper.selectCountByShops(any(), any(), any(), any())).thenReturn(1);
        when(mapper.selectListByShops(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of(header(1, "SHOP1")));

        var out = controller.shopList(null, null, 0, 20, shop("SHOP1"));

        assertEquals(1, out.get("total"));
        verify(mapper).selectListByShops(eq(Set.of("SHOP1")), eq(""), any(), any(), eq(0), eq(20));
    }

    @Test
    void 창고는_연결된_상점만_조회하고_허용_밖_상점을_지정하면_403이다() {
        when(mapper.selectShopsByWarehs("WH1")).thenReturn(List.of(Map.of("shopCode", "S1", "shopName", "가"), Map.of("shopCode", "S2", "shopName", "나")));
        when(mapper.selectCountByShops(any(), any(), any(), any())).thenReturn(0);
        when(mapper.selectListByShops(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of());
        MockHttpServletRequest wh = warehs("WH1");

        controller.warehsList(null, null, null, 0, 20, wh);
        controller.warehsList("S2", null, null, 0, 20, wh);
        assertThrows(ResponseStatusException.class, () -> controller.warehsList("OTHER", null, null, 0, 20, wh));

        verify(mapper, org.mockito.Mockito.times(2)).selectListByShops(any(), any(), any(), any(), anyInt(), anyInt());
        assertThat(controller.warehsShops(wh)).hasSize(2);
    }

    @Test
    void 연결된_상점이_없는_창고는_빈_목록이고_조회_쿼리를_타지_않는다() {
        when(mapper.selectShopsByWarehs("WH9")).thenReturn(List.of());

        var out = controller.warehsList(null, null, null, 0, 20, warehs("WH9"));

        assertEquals(0, out.get("total"));
        verify(mapper, never()).selectListByShops(any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void 발주서_다운로드는_번호가_필수이고_없으면_400이다() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.shopExcel(null, shop("SHOP1")));
        assertEquals(400, ex.getStatusCode().value());
        verify(autoOrderService, never()).download(any(), any(), any(), any(), eq(false));
    }

    @Test
    void 남의_상점_발주서나_없는_번호는_똑같이_404이고_내_것은_출력_횟수를_올리지_않고_받는다() {
        when(checkService.find(5)).thenReturn(header(5, "SHOP2"));
        when(checkService.find(6)).thenReturn(header(6, "SHOP1"));
        MockHttpServletRequest shop = shop("SHOP1");

        assertEquals(404, assertThrows(ResponseStatusException.class, () -> controller.shopExcel(5, shop)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> controller.shopExcel(99, shop)).getStatusCode().value());

        ResponseEntity<byte[]> ok = controller.shopExcel(6, shop);
        assertEquals(200, ok.getStatusCode().value());
        verify(autoOrderService).download(null, 6, "shopUser", "업체", false);   // bump=false
    }

    @Test
    void 창고는_연결된_상점의_발주서만_받는다() {
        when(mapper.selectShopsByWarehs("WH1")).thenReturn(List.of(Map.of("shopCode", "S1", "shopName", "가")));
        when(checkService.find(7)).thenReturn(header(7, "S1"));
        when(checkService.find(8)).thenReturn(header(8, "S3"));
        MockHttpServletRequest wh = warehs("WH1");

        assertEquals(200, controller.warehsExcel(7, wh).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> controller.warehsExcel(8, wh)).getStatusCode().value());
    }

    @Test
    void 세션이_다른_유형이면_허용_범위를_계산하지_못하고_403이다() {
        // 상점 세션 없이(예: 창고 세션만) 상점 경로 메서드가 호출된 경우를 방어한다 — 인터셉터가 먼저 401 로 막지만 컨트롤러도 열어 두지 않는다.
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.shopList(null, null, 0, 20, warehs("WH1"))).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.warehsShops(shop("SHOP1"))).getStatusCode().value());
    }

    @Test
    void 업체_경로는_관리자_보호_경로가_아니다_업체_세션_인터셉터가_따로_지킨다() {
        assertThat(MngrPaths.isMngrProtected("/vendor/shop/ship-check/list")).isFalse();
        assertThat(MngrPaths.isMngrProtected("/vendor/warehs/ship-check/excel")).isFalse();
        assertThat(MngrPaths.isMngrProtected("/ship/ship-check/list")).isTrue();   // 관리자용은 그대로 보호
    }
}
