package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderPayMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.payment.model.CancelRequest;
import com.shopwizard.payment.service.KcpGateway;
import com.shopwizard.payment.service.KcpProperties;
import com.shopwizard.payment.service.PaymentGateway;
import com.shopwizard.payment.service.PaymentGatewayRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 미입금 가상계좌 만료 정리 — 계좌를 먼저 닫고(성공해야) 주문을 취소한다. 닫지 못하면 주문은 그대로 두고, 마감 직전 입금은 구제한다.
 */
@ExtendWith(MockitoExtension.class)
class KcpVirtualAccountExpiryServiceTest {

    @Mock private OrderPayMapper orderPayMapper;
    @Mock private OrderService orderService;
    @Mock private OrderLineLock lineLock;
    @Mock private OrderPaymentSyncService paymentSync;
    @Mock private KcpGateway kcpGateway;
    @Mock private PaymentGatewayRegistry registry;
    @Mock private PlatformTransactionManager txManager;

    private KcpProperties props;
    private KcpVirtualAccountExpiryService service;

    @BeforeEach
    void setUp() {
        props = new KcpProperties();
        props.setVaExpireGraceMinutes(60);
        PaymentGateway kcp = mock(PaymentGateway.class);
        lenient().when(kcp.code()).thenReturn("KCP");
        lenient().when(registry.enabled()).thenReturn(List.of(kcp));
        service = new KcpVirtualAccountExpiryService(orderPayMapper, orderService, lineLock, paymentSync, kcpGateway, props, registry, txManager);
    }

    private static OrderPay pay(int orderNo) {
        OrderPay p = new OrderPay();
        p.setOrderNo(orderNo);
        p.setPayNo(1);
        p.setPayAmt(26900);
        p.setPgCode("KCP");
        p.setPgTradeNo("T-" + orderNo);
        p.setPayType("001000000000");
        return p;
    }

    private void orderState(int orderNo, String state) {
        Order o = new Order();
        o.setOrderNo(orderNo);
        o.setOrderState(state);
        lenient().when(orderService.select(Map.of("orderNo", orderNo))).thenReturn(o);
    }

    private void unpaid(int orderNo) {
        lenient().when(kcpGateway.queryDeposit("T-" + orderNo)).thenReturn(new KcpGateway.DepositStatus(0, "T123", "20261101100000"));
    }

    @Test
    void 마감이_지난_미입금_주문은_계좌를_사용중지한_뒤_주문을_취소하고_재고를_되돌린다() throws Exception {
        when(orderPayMapper.selectExpiredKcpVbank(60)).thenReturn(List.of(pay(1)));
        orderState(1, "주문접수");
        unpaid(1);

        int handled = service.run();

        assertThat(handled).isEqualTo(1);
        ArgumentCaptor<CancelRequest> cap = ArgumentCaptor.forClass(CancelRequest.class);
        InOrder order = inOrder(kcpGateway, paymentSync);
        order.verify(kcpGateway).cancel(cap.capture());              // 계좌를 먼저 닫고
        order.verify(paymentSync).cancelBeforeShipment(1, "SYSTEM", "KCP가상계좌만료");   // 그다음 주문 취소
        assertThat(cap.getValue().getTradeNo()).isEqualTo("T-1");
        assertThat(cap.getValue().getPayType()).isEqualTo("001000000000");
        verify(lineLock).lockHeader(1);
    }

    @Test
    void 사용중지에_실패하면_주문을_취소하지_않고_다음_주기에_다시_시도한다() throws Exception {
        when(orderPayMapper.selectExpiredKcpVbank(60)).thenReturn(List.of(pay(1)));
        orderState(1, "주문접수");
        unpaid(1);
        when(kcpGateway.cancel(any())).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "사용중지 실패"));

        int handled = service.run();

        assertThat(handled).isZero();
        verify(paymentSync, never()).cancelBeforeShipment(anyInt(), anyString(), anyString());
    }

    @Test
    void 마감_직전에_입금됐으면_취소하지_않고_지불완료로_구제한다() throws Exception {
        when(orderPayMapper.selectExpiredKcpVbank(60)).thenReturn(List.of(pay(1)));
        orderState(1, "주문접수");
        when(kcpGateway.queryDeposit("T-1")).thenReturn(new KcpGateway.DepositStatus(26900, "T123", "20261101100000"));

        int handled = service.run();

        assertThat(handled).isEqualTo(1);
        verify(paymentSync).markPaid(org.mockito.ArgumentMatchers.eq(1), any(Order.class), org.mockito.ArgumentMatchers.eq("SYSTEM"), anyString());
        verify(orderPayMapper).markDeposited(any());
        verify(kcpGateway, never()).cancel(any());
        verify(paymentSync, never()).cancelBeforeShipment(anyInt(), anyString(), anyString());
    }

    @Test
    void 입금액이_주문금액과_다르면_사용중지도_취소도_하지_않는다() throws Exception {
        when(orderPayMapper.selectExpiredKcpVbank(60)).thenReturn(List.of(pay(1)));
        orderState(1, "주문접수");
        when(kcpGateway.queryDeposit("T-1")).thenReturn(new KcpGateway.DepositStatus(5000, "T123", "20261101100000"));

        assertThat(service.run()).isZero();

        verify(kcpGateway, never()).cancel(any());
        verify(paymentSync, never()).markPaid(anyInt(), any(), anyString(), anyString());
        verify(paymentSync, never()).cancelBeforeShipment(anyInt(), anyString(), anyString());
    }

    @Test
    void 그_사이_이미_처리된_주문은_KCP_를_호출하지_않고_건너뛴다() throws Exception {
        when(orderPayMapper.selectExpiredKcpVbank(60)).thenReturn(List.of(pay(1)));
        orderState(1, "지불완료");

        assertThat(service.run()).isZero();

        verifyNoInteractions(kcpGateway, paymentSync);
    }

    @Test
    void 한_건이_실패해도_나머지는_계속_처리한다() throws Exception {
        when(orderPayMapper.selectExpiredKcpVbank(60)).thenReturn(List.of(pay(1), pay(2)));
        orderState(1, "주문접수");
        orderState(2, "주문접수");
        unpaid(1);
        unpaid(2);
        when(kcpGateway.cancel(org.mockito.ArgumentMatchers.argThat(c -> c != null && "T-1".equals(c.getTradeNo()))))
                .thenThrow(new IllegalStateException("KCP 오류"));

        int handled = service.run();

        assertThat(handled).isEqualTo(1);
        verify(paymentSync).cancelBeforeShipment(2, "SYSTEM", "KCP가상계좌만료");
        verify(paymentSync, never()).cancelBeforeShipment(org.mockito.ArgumentMatchers.eq(1), anyString(), anyString());
    }

    @Test
    void 잡이_꺼져_있으면_아무것도_하지_않는다() {
        props.setVaExpireJobEnabled(false);

        assertThat(service.run()).isZero();

        verifyNoInteractions(orderPayMapper, kcpGateway);
    }

    @Test
    void KCP_가_꺼져_있으면_아무것도_하지_않는다() {
        when(registry.enabled()).thenReturn(List.of());

        assertThat(service.run()).isZero();

        verifyNoInteractions(orderPayMapper, kcpGateway);
    }
}
