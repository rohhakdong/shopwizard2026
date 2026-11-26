package com.shopwizard.ship.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import com.shopwizard.order.web.OrderMasterGuard;
import com.shopwizard.profile.service.CustInfoLogService;
import com.shopwizard.ship.service.ShipGiftOptionRunService;
import com.shopwizard.ship.service.ShipPrintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 발주내역·발주내역2 컨트롤러 — 데이터 범위, 개인정보 조회 기록(목록·엑셀마다, 실패해도 조회는 성공), 생성 작업의 행위자. */
class ShipPrintControllerTest {

    private ShipPrintService service;
    private ShipGiftOptionRunService runService;
    private CustInfoLogService logService;
    private ShipPrintController controller;

    @BeforeEach
    void setUp() {
        DataScopeGuard scopeGuard = new DataScopeGuard(mock(DataScopeMapper.class));
        ReflectionTestUtils.setField(scopeGuard, "mode", "enforce");
        service = mock(ShipPrintService.class);
        runService = mock(ShipGiftOptionRunService.class);
        logService = mock(CustInfoLogService.class);
        when(service.list1(any(), any(), any(), any(), any(), any())).thenReturn(new ShipPrintService.Page(0, List.of()));
        when(service.list2(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new ShipPrintService.Page(0, List.of()));
        when(service.excel1(any(), any(), any(), any(), any())).thenReturn(new ShipPrintService.Download("a.xls", new byte[]{1}));
        when(service.excel2(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new ShipPrintService.Download("b.xls", new byte[]{1}));
        controller = new ShipPrintController(service, runService, logService, new OrderMasterGuard(scopeGuard));
    }

    private static MockHttpServletRequest login(Map<String, String> claims, String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        req.setRemoteAddr("10.1.2.3");
        AuthSession.login(req, AuthSession.Type.MNGR, claims);
        AuthSession.markActor(req, AuthSession.Type.MNGR);
        return req;
    }

    private static final Map<String, String> ALL = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자");
    private static final Map<String, String> SHOP = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자", "shopCode", "SHOP1");
    private static final Map<String, String> CHNL = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자", "chnlCode", "CH1");

    @Test
    void 목록_조회는_개인정보_조회_기록을_남긴다() {
        controller.list(Map.of("pShopCode", "SHOP1", "pShipCheckStartDate", "2026-11-01", "pShipCheckEndDate", "2026-11-02"),
                login(ALL, "/ship/print/list"));

        ArgumentCaptor<String> exec = ArgumentCaptor.forClass(String.class);
        verify(logService).record(eq("/ship/print/list"), exec.capture(), eq("shipPrintService.list1(map)"), eq("shipPrintMapper"),
                eq("tShpShipDirectPrint"), eq("u"), eq("사용자"), eq("10.1.2.3"));
        assertThat(exec.getValue()).contains("pShopCode=SHOP1").contains("pShipCheckStartDate=2026-11-01");
    }

    @Test
    void 목록_엑셀_네_경로_모두_기록한다() {
        controller.excel(Map.of("pShopCode", "SHOP1"), login(ALL, "/ship/print/excel"));
        controller.list2(Map.of("pShopCode", "SHOP1", "pProdName", "20L"), login(ALL, "/ship/print/list2"));
        controller.excel2(Map.of("pShopCode", "SHOP1"), login(ALL, "/ship/print/excel2"));

        verify(logService).record(eq("/ship/print/excel"), anyString(), eq("shipPrintService.excel1(map)"), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(logService).record(eq("/ship/print/list2"), anyString(), eq("shipPrintService.list2(map)"), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(logService).record(eq("/ship/print/excel2"), anyString(), eq("shipPrintService.excel2(map)"), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void 검증에_실패해_데이터를_읽지_못한_요청은_기록하지_않는다() {
        when(service.list1(any(), any(), any(), any(), any(), any())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "상점을 선택하세요."));

        assertThrows(ResponseStatusException.class, () -> controller.list(Map.of(), login(ALL, "/ship/print/list")));

        verify(logService, never()).record(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 기록이_실패해도_조회는_성공한다() {
        doThrow(new IllegalStateException("log db down")).when(logService).record(any(), any(), any(), any(), any(), any(), any(), any());

        ShipPrintService.Page page = controller.list(Map.of("pShopCode", "SHOP1"), login(ALL, "/ship/print/list"));
        ResponseEntity<byte[]> xls = controller.excel(Map.of("pShopCode", "SHOP1"), login(ALL, "/ship/print/excel"));

        assertThat(page.total()).isZero();
        assertThat(xls.getBody()).isNotEmpty();
        assertThat(xls.getHeaders().getContentDisposition().getFilename()).isEqualTo("a.xls");
    }

    @Test
    void 상점_제한_계정은_자기_상점만_채널_제한_계정은_못_본다() {
        MockHttpServletRequest shop = login(SHOP, "/ship/print/list");
        controller.list(Map.of("pShopCode", "SHOP1"), shop);

        assertThrows(ResponseStatusException.class, () -> controller.list(Map.of("pShopCode", "SHOP2"), shop));
        assertThrows(ResponseStatusException.class, () -> controller.excel(Map.of("pShopCode", "SHOP2"), shop));
        assertThrows(ResponseStatusException.class, () -> controller.list2(Map.of("pShopCode", "SHOP2"), shop));
        assertThrows(ResponseStatusException.class, () -> controller.excel2(Map.of("pShopCode", "SHOP2"), shop));
        assertThrows(ResponseStatusException.class, () -> controller.list(Map.of("pShopCode", "SHOP1"), login(CHNL, "/ship/print/list")));
        // 거부된 요청은 조회도 기록도 하지 않는다
        verify(service, never()).list1(eq("SHOP2"), any(), any(), any(), any(), any());
        verify(logService, never()).record(any(), org.mockito.ArgumentMatchers.contains("SHOP2"), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 페이지_값이_숫자가_아니면_400() {
        assertThrows(ResponseStatusException.class, () -> controller.list(Map.of("pShopCode", "SHOP1", "pPageSize", "abc"), login(ALL, "/ship/print/list")));
    }

    @Test
    void 생성_작업은_상점_범위를_검사하고_행위자를_세션에서_읽는다() {
        MockHttpServletRequest shop = login(SHOP, "/ship/print/gift-option-run");
        controller.giftOptionRun(Map.of("shopCode", "SHOP1", "startDate", "2026-11-01", "endDate", "2026-11-02"), shop);

        verify(runService).run("SHOP1", "2026-11-01", "2026-11-02", "u", "사용자");
        assertThrows(ResponseStatusException.class, () -> controller.giftOptionRun(Map.of("shopCode", "SHOP2", "startDate", "a", "endDate", "b"), shop));
        assertThrows(ResponseStatusException.class, () -> controller.giftOptionRuns("SHOP2", shop));
        assertThrows(ResponseStatusException.class, () -> controller.giftOptionRunDetails("SHOP2", 1, shop));
        verify(runService, never()).run(eq("SHOP2"), any(), any(), any(), any());
    }
}
