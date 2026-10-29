package com.shopwizard.order.service;

import com.shopwizard.order.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** PG 의 비동기 결과를 주문 상태에 반영하는 공용 로직(토스 웹훅 · KCP 웹훅 · KCP 가상계좌 만료 잡). */
@ExtendWith(MockitoExtension.class)
class OrderPaymentSyncServiceTest {

    @Mock private OrderService orderService;
    @Mock private OrderProdService orderProdService;
    @Mock private StockLedgerService stockLedgerService;

    private OrderPaymentSyncService sync;

    @BeforeEach
    void setUp() {
        sync = new OrderPaymentSyncService(orderService, orderProdService, stockLedgerService);
    }

    private static Order order(String state) {
        Order o = new Order();
        o.setOrderNo(10);
        o.setOrderState(state);
        return o;
    }

    @Test
    void 주문접수_주문은_헤더와_라인을_지불완료로_바꾼다() {
        boolean changed = sync.markPaid(10, order("주문접수"), "SYSTEM", "KCP웹훅");

        assertThat(changed).isTrue();
        ArgumentCaptor<Map<String, Object>> header = ArgumentCaptor.forClass(Map.class);
        verify(orderService).updateOrderState(header.capture());
        assertThat(header.getValue()).containsEntry("orderNo", 10).containsEntry("orderState", "지불완료")
                .containsEntry("changeId", "SYSTEM").containsEntry("changeName", "KCP웹훅");
        verify(orderProdService).updateOrderState(any());
    }

    @Test
    void 주문접수가_아니면_지불완료로_바꾸지_않는다() {
        for (String state : new String[] {"지불완료", "주문취소", "환불완료", "출고지시", "배송완료"}) {
            assertThat(sync.markPaid(10, order(state), "SYSTEM", "x")).isFalse();
        }
        assertThat(sync.markPaid(10, null, "SYSTEM", "x")).isFalse();
        verify(orderService, never()).updateOrderState(any());
        verify(orderProdService, never()).updateOrderState(any());
    }

    @Test
    void 배송_전_라인만_취소하고_취소된_라인만_재고를_되돌린다() {
        when(orderProdService.selectOrderProdNoList(10)).thenReturn(List.of(1, 2));
        when(orderProdService.cancelByWebhook(any())).thenReturn(1, 0);   // 2번 라인은 이미 반품/환불 단계라 건너뜀
        when(orderProdService.countOrderStateDiffer(any())).thenReturn(1);

        sync.cancelBeforeShipment(10, "SYSTEM", "KCP가상계좌만료");

        verify(stockLedgerService).restore(10, 1, "SYSTEM");
        verify(stockLedgerService, never()).restore(eq(10), eq(2), any());
        verify(orderService, never()).updateOrderCancel(any());   // 아직 취소되지 않은 라인이 남아 헤더는 승격하지 않는다
    }

    @Test
    void 모든_라인이_주문취소가_되면_헤더도_주문취소로_승격한다() {
        when(orderProdService.selectOrderProdNoList(10)).thenReturn(List.of(1));
        when(orderProdService.cancelByWebhook(any())).thenReturn(1);
        when(orderProdService.countOrderStateDiffer(any())).thenReturn(0);

        sync.cancelBeforeShipment(10, "SYSTEM", "KCP가상계좌만료");

        ArgumentCaptor<Order> cap = ArgumentCaptor.forClass(Order.class);
        verify(orderService).updateOrderCancel(cap.capture());
        assertThat(cap.getValue().getOrderNo()).isEqualTo(10);
        assertThat(cap.getValue().getOrderState()).isEqualTo("주문취소");
        assertThat(cap.getValue().getChangeName()).isEqualTo("KCP가상계좌만료");
    }
}
