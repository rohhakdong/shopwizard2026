package com.shopwizard.profile.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.profile.model.CustDeliAddr;
import com.shopwizard.profile.service.CustDeliAddrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 고객 배송지 API — custId 는 항상 서버 세션에서 나오고, 로그인하지 않으면 아무것도 하지 않는다. */
@ExtendWith(MockitoExtension.class)
class CustDeliAddrMeControllerTest {

    @Mock private CustDeliAddrService service;
    private CustDeliAddrMeController controller;

    @BeforeEach
    void setUp() {
        controller = new CustDeliAddrMeController(service);
    }

    private static MockHttpServletRequest loggedIn(int custId) {
        MockHttpServletRequest http = new MockHttpServletRequest();
        AuthSession.login(http, AuthSession.Type.CUST, Map.of("custId", String.valueOf(custId), "loginId", "cust01", "name", "홍고객"));
        return http;
    }

    @Test
    void 목록은_세션_회원의_것만_조회한다() {
        when(service.selectMine(2)).thenReturn(List.of(new CustDeliAddr()));

        assertThat(controller.list(loggedIn(2))).hasSize(1);

        verify(service).selectMine(2);
    }

    @Test
    void 추가할_때_바디의_custId_가_아니라_세션_custId_와_행위자를_넘긴다() {
        CustDeliAddr body = new CustDeliAddr();
        body.setCustId(999);   // 위조

        controller.add(body, loggedIn(2));

        verify(service).insertMine(2, body, "cust01", "홍고객");
    }

    @Test
    void 수정_기본지정_삭제도_세션_custId_로만_호출한다() {
        MockHttpServletRequest http = loggedIn(2);
        CustDeliAddr body = new CustDeliAddr();

        controller.update(7, body, http);
        controller.setDefault(7, http);
        controller.delete(7, http);

        verify(service).updateMine(2, 7, body, "cust01", "홍고객");
        verify(service).setDefaultMine(2, 7);
        verify(service).deleteMine(2, 7);
    }

    @Test
    void 로그인하지_않았으면_401이고_서비스를_부르지_않는다() {
        MockHttpServletRequest anonymous = new MockHttpServletRequest();

        for (Runnable call : List.<Runnable>of(
                () -> controller.list(anonymous),
                () -> controller.add(new CustDeliAddr(), anonymous),
                () -> controller.update(1, new CustDeliAddr(), anonymous),
                () -> controller.setDefault(1, anonymous),
                () -> controller.delete(1, anonymous))) {
            assertThatThrownBy(call::run)
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(401));
        }
        verifyNoInteractions(service);
    }

    @Test
    void 관리자_세션만_있고_고객_로그인이_없으면_401() {
        MockHttpServletRequest adminOnly = new MockHttpServletRequest();
        AuthSession.login(adminOnly, AuthSession.Type.MNGR, Map.of("loginId", "admin01", "name", "관리자"));

        assertThatThrownBy(() -> controller.list(adminOnly))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(401));
        verify(service, org.mockito.Mockito.never()).selectMine(org.mockito.ArgumentMatchers.anyInt());
    }
}
