package com.shopwizard.order.web;

import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.order.model.CheckoutProdItem;
import com.shopwizard.order.model.CheckoutQuoteRequest;
import com.shopwizard.order.model.CheckoutRequest;
import com.shopwizard.order.model.PricedLine;
import com.shopwizard.order.model.PricedOrder;
import com.shopwizard.order.service.CheckoutPricingService;
import com.shopwizard.order.service.CheckoutService;
import com.shopwizard.payment.service.TossPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 결제 콜백의 등록자(registId/registName)는 요청 바디가 아니라 로그인 고객 세션이 정한다. */
@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

    @Mock private TossPaymentService tossPaymentService;
    @Mock private CheckoutService checkoutService;
    @Mock private CheckoutPricingService pricingService;
    private CheckoutController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new CheckoutController(tossPaymentService, checkoutService, pricingService);
        // 서버 견적: 상품 P1(상점 S1) 1000원 × 1개 + 옵션 추가금 0, 배송비 0
        PricedLine line = new PricedLine();
        line.setProdCode("P1"); line.setProdName("서버상품명"); line.setShopCode("S1");
        line.setItemCode("20000"); line.setItemName("선택사항없음");
        line.setProdQty(1); line.setListPrice(1200); line.setSalePrice(1000); line.setOptionPrice(0);
        line.setUnitPrice(1000); line.setLineAmt(1000);
        PricedOrder priced = new PricedOrder();
        priced.setLines(List.of(line)); priced.setItemAmt(1000); priced.setDeliAmt(0); priced.setTotalAmt(1000);
        when(pricingService.price(any())).thenReturn(priced);
        lenient().when(tossPaymentService.confirm(anyString(), anyString(), anyLong())).thenReturn(Map.of());
        lenient().when(checkoutService.checkout(any(), any())).thenReturn(1001);
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

    @Test
    void 클라이언트_금액이_서버_견적과_다르면_토스_승인_호출_없이_400으로_거절한다() throws Exception {
        CheckoutRequest forged = req("x", "x");
        forged.setAmount(1L);          // 1000원 상품을 1원으로 결제하려는 위조
        forged.setTotalAmt(1);
        MockHttpServletRequest http = new MockHttpServletRequest("POST", "/order/checkout");

        assertThatThrownBy(() -> controller.checkout(forged, http))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));

        verify(tossPaymentService, never()).confirm(anyString(), anyString(), anyLong());
        verify(checkoutService, never()).checkout(any(), any());
    }

    @Test
    void amount_와_totalAmt_중_하나라도_서버_견적과_다르면_거절한다() throws Exception {
        CheckoutRequest r = req("x", "x");
        r.setTotalAmt(999);            // 토스 amount 는 맞지만 totalAmt 가 다름
        MockHttpServletRequest http = new MockHttpServletRequest("POST", "/order/checkout");

        assertThatThrownBy(() -> controller.checkout(r, http)).isInstanceOf(ResponseStatusException.class);
        verify(tossPaymentService, never()).confirm(anyString(), anyString(), anyLong());
    }

    @Test
    void 저장되는_라인은_클라이언트가_보낸_값이_아니라_서버_계산값이다() throws Exception {
        CheckoutRequest r = req("x", "x");
        r.getOrderProds().get(0).setProdName("위조상품명");
        r.getOrderProds().get(0).setSalePrice(1);
        r.getOrderProds().get(0).setShopCode("FORGED");
        MockHttpServletRequest http = new MockHttpServletRequest("POST", "/order/checkout");

        controller.checkout(r, http);

        CheckoutProdItem saved = captured().getOrderProds().get(0);
        assertThat(saved.getProdName()).isEqualTo("서버상품명");
        assertThat(saved.getSalePrice()).isEqualTo(1000);
        assertThat(saved.getOptionPrice()).isZero();
        assertThat(saved.getListPrice()).isEqualTo(1200);
        assertThat(saved.getShopCode()).isEqualTo("S1");
        assertThat(captured().getTotalAmt()).isEqualTo(1000);
    }

    @Test
    void 견적_엔드포인트는_서비스_계산_결과를_그대로_돌려준다() {
        CheckoutQuoteRequest q = new CheckoutQuoteRequest();
        q.setOrderProds(List.of(new CheckoutProdItem()));

        PricedOrder out = controller.quote(q);

        assertThat(out.getTotalAmt()).isEqualTo(1000);
        verify(pricingService).price(q.getOrderProds());
    }
}
