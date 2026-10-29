package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderPayMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.order.service.KcpWebhookService.Outcome;
import com.shopwizard.payment.service.KcpGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** KCP 가상계좌 입금 통보 처리 — 웹훅 본문이 아니라 KCP 거래조회로 확인한 입금만 반영하고, 모든 경로가 멱등이다. */
@ExtendWith(MockitoExtension.class)
class KcpWebhookServiceTest {

    private static final String TNO = "26931077341432";
    private static final int ORDER_NO = 2415100;

    @Mock private OrderPayMapper orderPayMapper;
    @Mock private OrderService orderService;
    @Mock private OrderLineLock lineLock;
    @Mock private OrderPaymentSyncService paymentSync;
    @Mock private KcpGateway kcpGateway;

    @InjectMocks private KcpWebhookService service;

    private static OrderPay pay(long amount) {
        OrderPay p = new OrderPay();
        p.setOrderNo(ORDER_NO);
        p.setPayNo(1);
        p.setPayAmt((int) amount);
        p.setPgCode("KCP");
        p.setPgTradeNo(TNO);
        return p;
    }

    private static Order order(String state) {
        Order o = new Order();
        o.setOrderNo(ORDER_NO);
        o.setOrderState(state);
        return o;
    }

    private void stubOrder(String state, long paidAtKcp) {
        when(orderPayMapper.selectByPgTradeNoAny("KCP", TNO)).thenReturn(pay(26900));
        when(kcpGateway.queryDeposit(TNO)).thenReturn(new KcpGateway.DepositStatus(paidAtKcp, "T0123456789", "20261101100000"));
        when(orderService.select(any())).thenReturn(order(state));
    }

    @Test
    void KCP_조회로_입금이_확인되면_지불완료로_바꾸고_입금_기록을_남긴다() {
        stubOrder("주문접수", 26900);
        when(paymentSync.markPaid(ORDER_NO, order("주문접수"), "SYSTEM", "KCP웹훅")).thenReturn(true);

        Outcome out = service.handleDeposit(TNO, 26900L);

        assertThat(out).isEqualTo(Outcome.PAID);
        verify(lineLock).lockHeader(ORDER_NO);
        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        verify(orderPayMapper).markDeposited(cap.capture());
        assertThat(cap.getValue()).containsEntry("orderNo", ORDER_NO).containsEntry("payNo", 1).containsEntry("amount", 26900L);
    }

    @Test
    void 우리_결제건이_아니거나_아직_저장_전이면_재전송을_받는다() {
        when(orderPayMapper.selectByPgTradeNoAny("KCP", TNO)).thenReturn(null);

        assertThat(service.handleDeposit(TNO, 26900L)).isEqualTo(Outcome.RETRY);

        verifyNoInteractions(kcpGateway, paymentSync, lineLock);
    }

    @Test
    void 이미_지불완료인_주문은_재전송이어도_아무것도_바꾸지_않는다() {
        stubOrder("지불완료", 26900);

        assertThat(service.handleDeposit(TNO, 26900L)).isEqualTo(Outcome.ALREADY_PROCESSED);

        verify(paymentSync, never()).markPaid(anyInt(), any(), any(), any());
        verify(orderPayMapper, never()).markDeposited(any());
    }

    @Test
    void 이미_주문취소된_주문에_입금되면_되살리지_않고_수동_처리로_남긴다() {
        stubOrder("주문취소", 26900);

        assertThat(service.handleDeposit(TNO, 26900L)).isEqualTo(Outcome.MANUAL);

        verify(paymentSync, never()).markPaid(anyInt(), any(), any(), any());
        verify(orderPayMapper, never()).markDeposited(any());
    }

    @Test
    void 취소된_주문에_입금이_없으면_그냥_넘어간다() {
        stubOrder("환불완료", 0);

        assertThat(service.handleDeposit(TNO, 0L)).isEqualTo(Outcome.ALREADY_PROCESSED);
    }

    @Test
    void 통보는_왔는데_KCP_조회상_입금이_없으면_통보를_믿지_않는다() {
        stubOrder("주문접수", 0);

        assertThat(service.handleDeposit(TNO, 26900L)).isEqualTo(Outcome.MANUAL);

        verify(paymentSync, never()).markPaid(anyInt(), any(), any(), any());
        verify(orderPayMapper, never()).markDeposited(any());
    }

    @Test
    void 입금액이_주문금액과_다르면_지불완료로_바꾸지_않는다() {
        stubOrder("주문접수", 20000);   // 부족 입금

        assertThat(service.handleDeposit(TNO, 20000L)).isEqualTo(Outcome.MANUAL);

        verify(paymentSync, never()).markPaid(anyInt(), any(), any(), any());
        verify(orderPayMapper, never()).markDeposited(any());
    }

    @Test
    void 통보_금액을_위조해도_KCP_조회_금액으로_판단한다() {
        stubOrder("주문접수", 26900);
        when(paymentSync.markPaid(anyInt(), any(), any(), any())).thenReturn(true);

        // 통보는 1원이라고 하지만 KCP 조회는 26,900원 입금 — 조회가 기준이다(경고만 남는다)
        assertThat(service.handleDeposit(TNO, 1L)).isEqualTo(Outcome.PAID);
    }

    @Test
    void KCP_조회가_실패하면_예외를_올려_재전송되게_한다() {
        when(orderPayMapper.selectByPgTradeNoAny("KCP", TNO)).thenReturn(pay(26900));
        when(kcpGateway.queryDeposit(TNO)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> service.handleDeposit(TNO, 26900L)).isInstanceOf(ResponseStatusException.class);

        verify(paymentSync, never()).markPaid(anyInt(), any(), any(), any());
    }

    @Test
    void 거래번호가_비어_있으면_수동_처리() {
        assertThat(service.handleDeposit(" ", 1L)).isEqualTo(Outcome.MANUAL);
        assertThat(service.handleDeposit(null, 1L)).isEqualTo(Outcome.MANUAL);
        verifyNoInteractions(orderPayMapper);
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
