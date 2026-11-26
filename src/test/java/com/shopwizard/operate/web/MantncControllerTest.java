package com.shopwizard.operate.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.framework.auth.mapper.DataScopeMapper;
import com.shopwizard.operate.model.ListPage;
import com.shopwizard.operate.model.MantncRequest;
import com.shopwizard.operate.service.MantncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 유지보수 컨트롤러 범위 — 상점 제한 403, 채널·서비스 제한은 자기 요청만(요청·수정·삭제 가능, 예정일·완료 403), 제한 없는 계정은 전체·처리. */
class MantncControllerTest {

    private MantncService service;
    private MantncController controller;

    @BeforeEach
    void setUp() {
        DataScopeGuard guard = new DataScopeGuard(mock(DataScopeMapper.class));
        ReflectionTestUtils.setField(guard, "mode", "enforce");
        service = mock(MantncService.class);
        when(service.list(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new ListPage<>(0, List.of()));
        when(service.summary(any())).thenReturn(Map.of("recipt", 1));
        controller = new MantncController(service, guard);
    }

    private static MockHttpServletRequest login(Map<String, String> claims) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthSession.login(req, AuthSession.Type.MNGR, claims);
        AuthSession.markActor(req, AuthSession.Type.MNGR);
        return req;
    }

    private static final Map<String, String> ALL = Map.of("svcAll", "Y", "loginId", "admin", "name", "관리자", "roleUid", "R1");
    private static final Map<String, String> SHOP = Map.of("svcAll", "Y", "loginId", "s", "name", "상점", "shopCode", "S1");
    private static final Map<String, String> CHNL = Map.of("svcAll", "Y", "loginId", "chnlUser", "name", "채널", "chnlCode", "CH1");
    private static final Map<String, String> SVC = Map.of("loginId", "svcUser", "name", "서비스", "svcCode", "SHP001");

    @Test
    void 제한_없는_계정은_전체를_보고_처리한다() {
        MockHttpServletRequest all = login(ALL);
        controller.list(Map.of(), all);
        verify(service).list(isNull(), any(), any(), any(), any(), any(), any(), any());

        Map<String, Object> s = controller.summary(all);
        assertThat(s).containsEntry("canHandle", true).containsEntry("loginId", "admin");

        MantncRequest r = new MantncRequest();
        controller.create(r, all);
        verify(service).create(r, "admin", "관리자", "R1");
        controller.schedule(r, all);
        controller.complete(r, all);
        verify(service).schedule(r, "admin", "관리자");
        verify(service).complete(r, "admin", "관리자");
    }

    @Test
    void 채널_서비스_제한_계정은_자기_요청만_다루고_예정일_완료는_403() {
        for (Map<String, String> limited : List.of(CHNL, SVC)) {
            MockHttpServletRequest req = login(limited);
            String me = limited.get("loginId");
            controller.list(Map.of(), req);
            verify(service).list(eq(me), any(), any(), any(), any(), any(), any(), any());
            assertThat(controller.summary(req)).containsEntry("canHandle", false);
            controller.get(5, req);
            verify(service).get(5, me);
            MantncRequest r = new MantncRequest();
            controller.create(r, req);
            controller.update(r, req);
            controller.delete(5, req);
            verify(service).update(eq(r), eq(me), eq(me), any());
            verify(service).delete(5, me);

            assertThrows(ResponseStatusException.class, () -> controller.schedule(r, req));
            assertThrows(ResponseStatusException.class, () -> controller.complete(r, req));
        }
        verify(service, never()).schedule(any(), any(), any());
        verify(service, never()).complete(any(), any(), any());
    }

    @Test
    void 상점_제한_계정은_모든_기능이_403() {
        MockHttpServletRequest shop = login(SHOP);
        MantncRequest r = new MantncRequest();
        assertThrows(ResponseStatusException.class, () -> controller.list(Map.of(), shop));
        assertThrows(ResponseStatusException.class, () -> controller.summary(shop));
        assertThrows(ResponseStatusException.class, () -> controller.get(1, shop));
        assertThrows(ResponseStatusException.class, () -> controller.create(r, shop));
        assertThrows(ResponseStatusException.class, () -> controller.update(r, shop));
        assertThrows(ResponseStatusException.class, () -> controller.delete(1, shop));
        assertThrows(ResponseStatusException.class, () -> controller.schedule(r, shop));
        assertThrows(ResponseStatusException.class, () -> controller.complete(r, shop));
    }

    @Test
    void 페이지_값이_숫자가_아니면_400() {
        assertThrows(ResponseStatusException.class, () -> controller.list(Map.of("pPageSize", "x"), login(ALL)));
    }
}
