package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderPayLogTossMapper;
import com.shopwizard.order.mapper.OrderPayMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.payment.service.TossPaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * TossWebhookService의 상태 동기화/멱등 처리 로직을
 * 실제 DB/토스 서버 없이 빠르게 검증하는 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class TossWebhookServiceTest {

    private static final String PAYMENT_KEY = "test-payment-key";
    private static final int ORDER_NO = 2414999;

    @Mock private TossPaymentService tossPaymentService;
    @Mock private OrderLineLock lineLock;
    @Mock private OrderPayMapper orderPayMapper;
    @Mock private OrderPayLogTossMapper orderPayLogTossMapper;
    @Mock private OrderService orderService;
    @Mock private OrderProdService orderProdService;

    @InjectMocks private TossWebhookService tossWebhookService;

    private OrderPay orderPay() {
        OrderPay pay = new OrderPay();
        pay.setOrderNo(ORDER_NO);
        return pay;
    }

    private Order orderWithState(String state) {
        Order order = new Order();
        order.setOrderNo(ORDER_NO);
        order.setOrderState(state);
        return order;
    }

    @Test
    void paymentKey가_비어있으면_아무것도_하지_않는다() throws Exception {
        tossWebhookService.handlePaymentStatusChanged(null);
        tossWebhookService.handlePaymentStatusChanged("");
        tossWebhookService.handlePaymentStatusChanged("   ");

        verifyNoInteractions(tossPaymentService, orderPayMapper, orderService, orderProdService, orderPayLogTossMapper);
    }

    @Test
    void 우리_시스템의_결제건이_아니면_무시한다() throws Exception {
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "DONE"));
        when(orderPayMapper.selectByPgTradeNo(PAYMENT_KEY)).thenReturn(null);

        tossWebhookService.handlePaymentStatusChanged(PAYMENT_KEY);

        verifyNoInteractions(orderService, orderProdService, orderPayLogTossMapper);
    }

    @Test
    void 주문을_찾을수없으면_무시한다() throws Exception {
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "DONE"));
        when(orderPayMapper.selectByPgTradeNo(PAYMENT_KEY)).thenReturn(orderPay());
        when(orderService.select(any())).thenReturn(null);

        tossWebhookService.handlePaymentStatusChanged(PAYMENT_KEY);

        verify(orderService, never()).updateOrderState(any());
        verifyNoInteractions(orderProdService, orderPayLogTossMapper);
    }

    @Test
    void DONE이면_주문접수_상태를_지불완료로_갱신한다() throws Exception {
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "DONE", "totalAmount", 26900L));
        when(orderPayMapper.selectByPgTradeNo(PAYMENT_KEY)).thenReturn(orderPay());
        when(orderService.select(any())).thenReturn(orderWithState("주문접수"));

        tossWebhookService.handlePaymentStatusChanged(PAYMENT_KEY);

        ArgumentCaptor<Map<String, Object>> paramsCaptor = captorForMap();
        verify(orderService).updateOrderState(paramsCaptor.capture());
        assertThat(paramsCaptor.getValue().get("orderState")).isEqualTo("지불완료");
        assertThat(paramsCaptor.getValue().get("orderNo")).isEqualTo(ORDER_NO);
        verify(orderProdService).updateOrderState(any());
        verify(orderPayLogTossMapper, times(1)).insert(any());
    }

    @Test
    void 이미_지불완료면_DONE_웹훅을_다시받아도_아무것도_안한다() throws Exception {
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "DONE", "totalAmount", 26900L));
        when(orderPayMapper.selectByPgTradeNo(PAYMENT_KEY)).thenReturn(orderPay());
        when(orderService.select(any())).thenReturn(orderWithState("지불완료"));

        tossWebhookService.handlePaymentStatusChanged(PAYMENT_KEY);

        verify(orderService, never()).updateOrderState(any());
        verifyNoInteractions(orderProdService);
        verify(orderPayLogTossMapper, never()).insert(any());
    }

    @Test
    void CANCELED면_배송전_라인을_주문취소로_바꾸고_모두_취소되면_헤더도_승격한다() throws Exception {
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "CANCELED", "totalAmount", 26900L));
        when(orderPayMapper.selectByPgTradeNo(PAYMENT_KEY)).thenReturn(orderPay());
        when(orderService.select(any())).thenReturn(orderWithState("지불완료"));
        when(orderProdService.selectOrderProdNoList(ORDER_NO)).thenReturn(List.of(1, 2));
        when(orderProdService.countOrderStateDiffer(any())).thenReturn(0);

        tossWebhookService.handlePaymentStatusChanged(PAYMENT_KEY);

        // 라인은 조건부 SQL(배송 전 상태만)로 갱신 — 무조건 갱신하는 updateOrderState 는 쓰지 않는다
        verify(orderProdService, times(2)).cancelByWebhook(any());
        verify(orderProdService, never()).updateOrderState(any());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).updateOrderCancel(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderState()).isEqualTo("주문취소");
        verify(orderPayLogTossMapper, times(1)).insert(any());
    }

    @Test
    void CANCELED여도_반품_환불_단계_라인이_남아있으면_헤더는_승격하지_않는다() throws Exception {
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "CANCELED", "totalAmount", 26900L));
        when(orderPayMapper.selectByPgTradeNo(PAYMENT_KEY)).thenReturn(orderPay());
        when(orderService.select(any())).thenReturn(orderWithState("지불완료"));
        when(orderProdService.selectOrderProdNoList(ORDER_NO)).thenReturn(List.of(1, 2));
        // 한 라인은 이미 반품완료/환불완료라 cancelByWebhook 이 건드리지 못했다 → 주문취소가 아닌 라인이 남음
        when(orderProdService.countOrderStateDiffer(any())).thenReturn(1);

        tossWebhookService.handlePaymentStatusChanged(PAYMENT_KEY);

        verify(orderProdService, times(2)).cancelByWebhook(any());
        verify(orderService, never()).updateOrderCancel(any());
        verify(orderPayLogTossMapper, times(1)).insert(any());
    }

    @Test
    void 이미_주문취소면_CANCELED_웹훅을_다시받아도_아무것도_안한다() throws Exception {
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "CANCELED", "totalAmount", 26900L));
        when(orderPayMapper.selectByPgTradeNo(PAYMENT_KEY)).thenReturn(orderPay());
        when(orderService.select(any())).thenReturn(orderWithState("주문취소"));

        tossWebhookService.handlePaymentStatusChanged(PAYMENT_KEY);

        verify(orderService, never()).updateOrderCancel(any());
        verifyNoInteractions(orderProdService);
        verify(orderPayLogTossMapper, never()).insert(any());
    }

    @Test
    void 환불확정이_이미_환불완료로_바꾼_주문은_취소_웹훅이_덮어쓰지_않는다() throws Exception {
        // 환불확정이 토스 취소를 호출하면 토스가 곧바로 CANCELED 웹훅을 보낸다 — 이때 상태가 되돌아가면 안 된다.
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "CANCELED", "totalAmount", 26900L));
        when(orderPayMapper.selectByPgTradeNo(PAYMENT_KEY)).thenReturn(orderPay());
        when(orderService.select(any())).thenReturn(orderWithState("환불완료"));

        tossWebhookService.handlePaymentStatusChanged(PAYMENT_KEY);

        verify(orderService, never()).updateOrderCancel(any());
        verifyNoInteractions(orderProdService);
        verify(orderPayLogTossMapper, never()).insert(any());
    }

    @Test
    void 부분취소등_그외_상태는_주문상태는_안바꾸고_로그만_남긴다() throws Exception {
        when(tossPaymentService.getPayment(PAYMENT_KEY)).thenReturn(Map.of("status", "PARTIAL_CANCELED", "totalAmount", 26900L));
        when(orderPayMapper.selectByPgTradeNo(PAYMENT_KEY)).thenReturn(orderPay());
        when(orderService.select(any())).thenReturn(orderWithState("지불완료"));

        tossWebhookService.handlePaymentStatusChanged(PAYMENT_KEY);

        verify(orderService, never()).updateOrderState(any());
        verify(orderService, never()).updateOrderCancel(any());
        verifyNoInteractions(orderProdService);
        verify(orderPayLogTossMapper, times(1)).insert(any());
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Map<String, Object>> captorForMap() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
