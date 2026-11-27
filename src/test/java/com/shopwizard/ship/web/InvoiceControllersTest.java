package com.shopwizard.ship.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import com.shopwizard.ship.model.SlnkResendRequest;
import com.shopwizard.ship.service.InvoiceExcelService;
import com.shopwizard.ship.service.ShipDirectService;
import com.shopwizard.ship.service.SlnkInvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 송장입력 컨트롤러 — 저장 뒤에만 샵링커 전송, 0건이면 전송 안 함, 송장 재전송·엑셀의 데이터 범위. */
class InvoiceControllersTest {

    private ShipDirectService shipDirectService;
    private SlnkInvoiceService slnk;
    private InvoiceExcelService excel;
    private DataScopeGuard guard;
    private ShipDirectController direct;
    private InvoiceExcelController excelController;

    @BeforeEach
    void setUp() {
        guard = new DataScopeGuard(mock(DataScopeMapper.class));
        ReflectionTestUtils.setField(guard, "mode", "enforce");
        shipDirectService = mock(ShipDirectService.class);
        slnk = mock(SlnkInvoiceService.class);
        excel = mock(InvoiceExcelService.class);
        direct = new ShipDirectController(shipDirectService, guard, slnk);
        excelController = new InvoiceExcelController(excel, slnk, guard);
        when(slnk.mode()).thenReturn("dry-run");
    }

    private static MockHttpServletRequest login(Map<String, String> claims) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, claims);
        AuthSession.markActor(req, AuthSession.Type.MNGR);
        return req;
    }

    private static final Map<String, String> ALL = Map.of("svcAll", "Y", "loginId", "u", "name", "사용자");

    private static Map<String, Object> body() {
        Map<String, Object> p = new HashMap<>();
        p.put("orderNo", 100);
        p.put("orderProdNo", 1);
        p.put("orderChangeNo", 2);
        p.put("deliCompName", "CJ-GLS택배");
        p.put("invNo", "123");
        return p;
    }

    @Test
    void 송장이_저장되면_샵링커_전송_결과를_함께_돌려준다() {
        when(shipDirectService.updateInvoice(anyMap())).thenReturn(1);
        when(slnk.pushLine(eq(100), eq(1), eq(2), any())).thenReturn(new SlnkInvoiceService.Result("sent", "샵링커에 전송했습니다."));

        Map<String, Object> out = direct.updateInvoice(body(), login(ALL));

        assertThat(out).containsEntry("updated", 1);
        assertThat(out.get("slnk")).isEqualTo(Map.of("status", "sent", "message", "샵링커에 전송했습니다."));
    }

    @Test
    void 저장되지_않으면_전송하지_않는다() {
        when(shipDirectService.updateInvoice(anyMap())).thenReturn(0);

        Map<String, Object> out = direct.updateInvoice(body(), login(ALL));

        assertThat(out).containsEntry("updated", 0).doesNotContainKey("slnk");
        verify(slnk, never()).pushLine(any(), any(), any(), any());
    }

    @Test
    void 재전송은_줄_범위를_검사하고_개수_상한과_필수값을_검사한다() {
        SlnkResendRequest req = new SlnkResendRequest();
        SlnkResendRequest.Line l = new SlnkResendRequest.Line();
        l.setOrderNo(1);
        l.setOrderProdNo(1);
        l.setOrderChangeNo(1);
        req.setLines(List.of(l));
        when(slnk.resend(anyList(), any())).thenReturn(Map.of("1-1-1", new SlnkInvoiceService.Result("dry", "x")));

        Map<String, Object> out = excelController.resend(req, login(ALL));
        assertThat(out).containsEntry("mode", "dry-run").containsKey("results");

        req.setLines(List.of());
        assertThrows(ResponseStatusException.class, () -> excelController.resend(req, login(ALL)));
        SlnkResendRequest big = new SlnkResendRequest();
        for (int i = 0; i <= SlnkInvoiceService.MAX_RESEND; i++) big.getLines().add(l);
        assertThrows(ResponseStatusException.class, () -> excelController.resend(big, login(ALL)));
        SlnkResendRequest partial = new SlnkResendRequest();
        SlnkResendRequest.Line bad = new SlnkResendRequest.Line();
        bad.setOrderNo(1);
        partial.setLines(List.of(bad));
        assertThrows(ResponseStatusException.class, () -> excelController.resend(partial, login(ALL)));
    }

    @Test
    void 미전송_조회는_범위_파라미터를_서비스로_넘긴다() {
        when(slnk.pending(any(), any(), any())).thenReturn(Map.of("total", 0));
        excelController.pending(Map.of("pShopCode", "S1", "pSvcCode", "SHP001"));
        verify(slnk).pending("S1", null, "SHP001");
    }
}
