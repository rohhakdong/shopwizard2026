package com.shopwizard.order.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import com.shopwizard.order.model.ManualOrderRequest;
import com.shopwizard.order.model.ManualOrderResult;
import com.shopwizard.order.service.ManualOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 수기 주문 등록의 데이터 범위 — 채널 제한 계정은 자기 채널로, 서비스·상점 제한 계정은 범위 밖 채널·상품을 담을 수 없다. */
class ManualOrderControllerTest {

    private DataScopeMapper mapper;
    private ManualOrderService service;
    private ManualOrderController controller;

    @BeforeEach
    void setUp() {
        mapper = mock(DataScopeMapper.class);
        DataScopeGuard guard = new DataScopeGuard(mapper);
        ReflectionTestUtils.setField(guard, "mode", "enforce");
        service = mock(ManualOrderService.class);
        controller = new ManualOrderController(service, guard);
    }

    private static MockHttpServletRequest login(Map<String, String> claims) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, claims);
        return req;
    }

    private static ManualOrderRequest request(String chnlCode, String... prodCodes) {
        ManualOrderRequest r = new ManualOrderRequest();
        r.setChnlCode(chnlCode);
        r.setLines(java.util.Arrays.stream(prodCodes).map(c -> {
            ManualOrderRequest.Line l = new ManualOrderRequest.Line();
            l.setProdCode(c);
            return l;
        }).toList());
        return r;
    }

    @Test
    void 제한_없는_계정은_그대로_서비스로_간다() {
        ManualOrderRequest req = request("CH1", "P1");
        ManualOrderResult result = new ManualOrderResult(1, 1, 1000);
        when(service.create(req)).thenReturn(result);

        assertEquals(result, controller.create(req, login(Map.of("svcAll", "Y", "loginId", "u"))));
    }

    @Test
    void 채널_제한_계정은_body_의_채널을_믿지_않고_자기_채널로_등록한다() {
        ManualOrderRequest req = request("OTHER", "P1");

        controller.create(req, login(Map.of("svcAll", "Y", "loginId", "u", "chnlCode", "MINE")));

        assertEquals("MINE", req.getChnlCode());
        verify(service).create(req);
    }

    @Test
    void 서비스_제한_계정이_다른_서비스_채널을_고르면_403() {
        when(mapper.countChnlInSvc("FOREIGN", "SHP001")).thenReturn(0);
        when(mapper.countChnlInSvc("MINE", "SHP001")).thenReturn(1);
        MockHttpServletRequest login = login(Map.of("loginId", "u", "svcCode", "SHP001"));

        assertThrows(ResponseStatusException.class, () -> controller.create(request("FOREIGN"), login));
        verify(service, never()).create(any());

        controller.create(request("MINE"), login);
        verify(service).create(any());
    }

    @Test
    void 상점_제한_계정은_다른_상점_상품이_한_줄이라도_섞이면_서비스를_호출하지_않고_403() {
        when(mapper.countPrdProd("MINE-P", "SHOP1", null)).thenReturn(1);
        when(mapper.countPrdProd("OTHER-P", "SHOP1", null)).thenReturn(0);
        MockHttpServletRequest login = login(Map.of("svcAll", "Y", "loginId", "u", "shopCode", "SHOP1"));

        assertThrows(ResponseStatusException.class, () -> controller.create(request("CH1", "MINE-P", "OTHER-P"), login));
        verify(service, never()).create(any());

        controller.create(request("CH1", "MINE-P"), login);
        verify(service).create(any());
    }

    @Test
    void 상품_줄이_없는_요청은_서비스가_거절하도록_그대로_넘긴다() {
        ManualOrderRequest req = new ManualOrderRequest();
        req.setChnlCode("CH1");
        req.setLines(List.of());

        controller.create(req, login(Map.of("svcAll", "Y", "loginId", "u")));

        verify(service).create(req);
    }
}
