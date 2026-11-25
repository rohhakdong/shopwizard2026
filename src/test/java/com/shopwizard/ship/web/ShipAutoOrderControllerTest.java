package com.shopwizard.ship.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import com.shopwizard.order.web.OrderMasterGuard;
import com.shopwizard.order.service.KeywordTargetService;
import com.shopwizard.order.service.OrderFeeApplyService;
import com.shopwizard.ship.model.ShipCheck;
import com.shopwizard.ship.service.ShipAutoOrderService;
import com.shopwizard.ship.service.ShipCheckService;
import com.shopwizard.ship.service.ShipDataBuildService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 자동발주·발주 목록의 데이터 범위 — 상점 제한은 자기 상점만, 채널 제한은 불가, 서비스 제한은 자기 서비스의 상점만. */
class ShipAutoOrderControllerTest {

    private DataScopeMapper scopeMapper;
    private ShipAutoOrderService service;
    private ShipDataBuildService buildService;
    private ShipCheckService checkService;
    private OrderFeeApplyService feeApplyService;
    private KeywordTargetService keywordService;
    private ShipAutoOrderController controller;
    private ShipCheckController checkController;

    @BeforeEach
    void setUp() {
        scopeMapper = mock(DataScopeMapper.class);
        DataScopeGuard scopeGuard = new DataScopeGuard(scopeMapper);
        ReflectionTestUtils.setField(scopeGuard, "mode", "enforce");
        OrderMasterGuard guard = new OrderMasterGuard(scopeGuard);
        service = mock(ShipAutoOrderService.class);
        when(service.download(any(), any(), any(), any())).thenReturn(new ShipAutoOrderService.Download("a.xls", new byte[0]));
        buildService = mock(ShipDataBuildService.class);
        checkService = mock(ShipCheckService.class);
        feeApplyService = mock(OrderFeeApplyService.class);
        keywordService = mock(KeywordTargetService.class);
        controller = new ShipAutoOrderController(service, buildService, feeApplyService, keywordService, guard);
        checkController = new ShipCheckController(checkService, guard);
    }

    private static MockHttpServletRequest login(Map<String, String> claims) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, claims);
        AuthSession.markActor(req, AuthSession.Type.MNGR);
        return req;
    }

    private static final Map<String, String> ALL = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자");
    private static final Map<String, String> SHOP = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자", "shopCode", "SHOP1");
    private static final Map<String, String> CHNL = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자", "chnlCode", "CH1");
    private static final Map<String, String> SVC = Map.of("loginId", "u", "name", "사용자", "svcCode", "SHP001");

    @Test
    void 제한_없는_계정은_어느_상점이든_처리하고_행위자는_세션에서_읽는다() {
        controller.build(Map.of("shopCode", "ANY"), login(ALL));

        verify(buildService).build("ANY", "u", "사용자");
    }

    @Test
    void 상점_제한_계정은_자기_상점만_현황_발주서_생성_다운로드를_할_수_있다() {
        MockHttpServletRequest shop = login(SHOP);

        controller.summary("SHOP1", shop);
        controller.sheet("SHOP1", 0, 50, shop);
        controller.pendingLines("SHOP1", shop);
        controller.sheetExcel("SHOP1", null, shop);
        controller.build(Map.of("shopCode", "SHOP1"), shop);

        assertThrows(ResponseStatusException.class, () -> controller.summary("SHOP2", shop));
        assertThrows(ResponseStatusException.class, () -> controller.sheet("SHOP2", 0, 50, shop));
        assertThrows(ResponseStatusException.class, () -> controller.pendingLines("SHOP2", shop));
        assertThrows(ResponseStatusException.class, () -> controller.sheetExcel("SHOP2", null, shop));
        assertThrows(ResponseStatusException.class, () -> controller.build(Map.of("shopCode", "SHOP2"), shop));
        verify(buildService, org.mockito.Mockito.times(1)).build(any(), any(), any());
    }

    @Test
    void 수수료_규칙_적용도_상점_범위를_검사하고_행위자는_세션에서_읽는다() {
        MockHttpServletRequest shop = login(SHOP);

        controller.applyFee(Map.of("shopCode", "SHOP1"), shop);
        assertThrows(ResponseStatusException.class, () -> controller.applyFee(Map.of("shopCode", "SHOP2"), shop));
        assertThrows(ResponseStatusException.class, () -> controller.applyFee(Map.of("shopCode", "SHOP1"), login(CHNL)));

        verify(feeApplyService, org.mockito.Mockito.times(1)).apply("SHOP1", "u", "사용자");
    }

    @Test
    void 키워드_가져오기도_상점_범위를_검사하고_행위자는_세션에서_읽는다() {
        MockHttpServletRequest shop = login(SHOP);

        controller.keywordPreview("SHOP1", shop);
        controller.keywordAssign(Map.of("shopCode", "SHOP1"), shop);
        assertThrows(ResponseStatusException.class, () -> controller.keywordPreview("SHOP2", shop));
        assertThrows(ResponseStatusException.class, () -> controller.keywordAssign(Map.of("shopCode", "SHOP2"), shop));
        assertThrows(ResponseStatusException.class, () -> controller.keywordAssign(Map.of("shopCode", "SHOP1"), login(CHNL)));

        verify(keywordService).preview("SHOP1");
        verify(keywordService, org.mockito.Mockito.times(1)).assign("SHOP1", "u", "사용자");
    }

    @Test
    void 채널_제한_계정은_자동발주를_쓰지_못한다() {
        MockHttpServletRequest chnl = login(CHNL);

        assertThrows(ResponseStatusException.class, () -> controller.summary("SHOP1", chnl));
        assertThrows(ResponseStatusException.class, () -> controller.build(Map.of("shopCode", "SHOP1"), chnl));
        verify(buildService, never()).build(any(), any(), any());
    }

    @Test
    void 서비스_제한_계정은_자기_서비스의_상점만_다룬다() {
        when(scopeMapper.countShopInSvc("MINE", "SHP001")).thenReturn(1);
        when(scopeMapper.countShopInSvc("FOREIGN", "SHP001")).thenReturn(0);
        MockHttpServletRequest svc = login(SVC);

        controller.build(Map.of("shopCode", "MINE"), svc);
        assertThrows(ResponseStatusException.class, () -> controller.build(Map.of("shopCode", "FOREIGN"), svc));
        verify(buildService, org.mockito.Mockito.times(1)).build(any(), any(), any());
    }

    @Test
    void 발주_목록_건을_다시_받을_때는_그_건의_상점으로_범위를_검사한다() {
        when(service.shopOfCheck(5)).thenReturn("SHOP2");
        when(service.shopOfCheck(6)).thenReturn("SHOP1");
        MockHttpServletRequest shop = login(SHOP);

        assertThrows(ResponseStatusException.class, () -> controller.sheetExcel(null, 5, shop));
        controller.sheetExcel(null, 6, shop);
        verify(service).download(null, 6, "u", "사용자");
        verify(service, never()).download(null, 5, "u", "사용자");
    }

    @Test
    void 발주_목록_조회는_상점_제한이면_자기_상점_채널_제한은_불가_단건은_그_행의_상점으로_검사한다() {
        MockHttpServletRequest shop = login(SHOP);
        when(checkService.select(Map.of("shipCheckNo", "9"))).thenReturn(check("SHOP2"));
        when(checkService.select(Map.of("shipCheckNo", "8"))).thenReturn(check("SHOP1"));
        when(checkService.selectList(any())).thenReturn(List.of());

        checkController.selectList(Map.of("pShopCode", "SHOP1"), shop);
        assertThrows(ResponseStatusException.class, () -> checkController.selectList(Map.of("pShopCode", "SHOP2"), shop));
        assertThrows(ResponseStatusException.class, () -> checkController.select(Map.of("shipCheckNo", "9"), shop));
        checkController.select(Map.of("shipCheckNo", "8"), shop);

        MockHttpServletRequest chnl = login(CHNL);
        assertThrows(ResponseStatusException.class, () -> checkController.selectCount(Map.of(), chnl));
    }

    private static ShipCheck check(String shop) {
        ShipCheck c = new ShipCheck();
        c.setShopCode(shop);
        return c;
    }
}
