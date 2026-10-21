package com.shopwizard.order.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.order.model.CheckoutProdItem;
import com.shopwizard.order.model.CheckoutRequest;
import com.shopwizard.order.service.CheckoutService;
import com.shopwizard.payment.service.TossPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 결제 콜백의 등록자(registId/registName)는 요청 바디가 아니라 로그인 고객 세션이 정한다. */
@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

    @Mock private TossPaymentService tossPaymentService;
    @Mock private CheckoutService checkoutService;
    private CheckoutController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new CheckoutController(tossPaymentService, checkoutService);
        when(tossPaymentService.confirm(anyString(), anyString(), anyLong())).thenReturn(Map.of());
        when(checkoutService.checkout(any(), any())).thenReturn(1001);
    }

    private static CheckoutRequest req(String registId, String registName) {
        CheckoutRequest r = new CheckoutRequest();
        r.setPaymentKey("pk");
        r.setOrderId("SW-1");
        r.setAmount(1000L);
        r.setTotalAmt(1000);
        r.setRegistId(registId);
        r.setRegistName(registName);
        CheckoutProdItem item = new CheckoutProdItem();
        item.setProdCode("P1");
        item.setProdQty(1);
        item.setSalePrice(1000);
        r.setOrderProds(List.of(item));
        return r;
    }

    private CheckoutRequest captured() throws Exception {
        ArgumentCaptor<CheckoutRequest> c = ArgumentCaptor.forClass(CheckoutRequest.class);
        verify(checkoutService).checkout(c.capture(), any());
        return c.getValue();
    }

    @Test
    void 로그인_고객은_바디의_registId_를_버리고_세션의_로그인ID_이름으로_저장된다() throws Exception {
        MockHttpServletRequest http = new MockHttpServletRequest("POST", "/order/checkout");
        AuthSession.login(http, AuthSession.Type.CUST, Map.of("custId", "2", "loginId", "cust01", "name", "홍고객"));

        controller.checkout(req("admin-FORGED", "위조"), http);

        CheckoutRequest saved = captured();
        assertThat(saved.getCustId()).isEqualTo(2);
        assertThat(saved.getRegistId()).isEqualTo("cust01");
        assertThat(saved.getRegistName()).isEqualTo("홍고객");
    }

    @Test
    void 고객_이름이_비어있으면_로그인ID_로_대신한다() throws Exception {
        MockHttpServletRequest http = new MockHttpServletRequest("POST", "/order/checkout");
        AuthSession.login(http, AuthSession.Type.CUST, Map.of("custId", "2", "loginId", "cust01"));

        controller.checkout(req("x", "x"), http);

        assertThat(captured().getRegistName()).isEqualTo("cust01");
    }

    @Test
    void 비회원은_바디의_registId_를_비워서_서비스가_GUEST_규칙을_적용하게_한다() throws Exception {
        MockHttpServletRequest http = new MockHttpServletRequest("POST", "/order/checkout");   // 세션 없음

        controller.checkout(req("admin-FORGED", "위조"), http);

        CheckoutRequest saved = captured();
        assertThat(saved.getCustId()).isZero();
        assertThat(saved.getRegistId()).isNull();
        assertThat(saved.getRegistName()).isNull();
    }

    @Test
    void 관리자_세션만_있고_고객_로그인이_없으면_비회원으로_취급하고_관리자_ID_가_섞이지_않는다() throws Exception {
        MockHttpServletRequest http = new MockHttpServletRequest("POST", "/order/checkout");
        AuthSession.login(http, AuthSession.Type.MNGR, Map.of("loginId", "admin01", "name", "관리자"));

        controller.checkout(req("x", "x"), http);

        CheckoutRequest saved = captured();
        assertThat(saved.getCustId()).isZero();
        assertThat(saved.getRegistId()).isNull();
    }
}
