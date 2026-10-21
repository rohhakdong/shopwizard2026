package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderProd;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 주문취소접수(지불완료/출고지시/발주확인 → 주문취소) 1건 처리. */
@ExtendWith(MockitoExtension.class)
class OrderCancelServiceTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderLineLock lineLock;
    @Mock private OrderProdMapper orderProdMapper;
    @Mock private StockLedgerService stockLedgerService;
    @InjectMocks private OrderCancelService service;

    private void givenLine(String state) {
        OrderProd op = new OrderProd();
        op.setOrderState(state);
        when(lineLock.lockHeaderAndLine(any(), any())).thenReturn(op);
    }

    @Test
    void 주문라인이_없으면_아무것도_바꾸지_않는다() {
        when(lineLock.lockHeaderAndLine(any(), any())).thenReturn(null);

        assertThat(service.cancelOne(1, 1, "단순변심", "", "admin", "관리자")).contains("찾을 수 없습니다");

        verify(orderProdMapper, never()).updateReturnReason(any());
        verifyNoInteractions(orderMapper, stockLedgerService);
    }

    @Test
    void 취소_가능한_상태만_취소한다() {
        for (String state : new String[]{"지불완료", "출고지시", "발주확인"}) {
            givenLine(state);
            when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1);
            assertThat(service.cancelOne(1, 1, "단순변심", "", "admin", "관리자")).as(state).isNull();
        }
    }

    @Test
    void 배송이_시작된_건이나_이미_취소된_건은_거부하고_아무것도_바꾸지_않는다() {
        for (String state : new String[]{"배송시작", "배송완료", "반품요청", "주문취소", "환불완료", "결제오류"}) {
            givenLine(state);
            assertThat(service.cancelOne(1, 1, "단순변심", "", "admin", "관리자")).as(state)
                    .contains("취소 가능한 상태가 아닙니다").contains(state);
        }
        verify(orderProdMapper, never()).updateReturnReason(any());
        verifyNoInteractions(orderMapper, stockLedgerService);
    }

    @Test
    void 사유와_주문취소_상태를_기록하고_재고_복원을_호출한다() {
        givenLine("지불완료");
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1);

        service.cancelOne(7, 3, "품절", "재고없음", "admin", "관리자");

        ArgumentCaptor<OrderProd> upd = ArgumentCaptor.forClass(OrderProd.class);
        verify(orderProdMapper).updateReturnReason(upd.capture());
        assertThat(upd.getValue().getOrderState()).isEqualTo("주문취소");
        assertThat(upd.getValue().getReturnReason()).isEqualTo("품절");
        assertThat(upd.getValue().getReturnReasonDetail()).isEqualTo("재고없음");
        assertThat(upd.getValue().getOrderNo()).isEqualTo(7);
        assertThat(upd.getValue().getOrderProdNo()).isEqualTo(3);
        verify(stockLedgerService).restore(7, 3, "admin");
    }

    @Test
    void 다른_라인이_아직_남아있으면_주문_헤더는_승격하지_않는다() {
        givenLine("지불완료");
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1);   // 취소 아닌 라인이 1개 남음

        service.cancelOne(7, 1, "단순변심", "", "admin", "관리자");

        verify(orderMapper, never()).updateOrderCancel(any());
    }

    @Test
    void 모든_라인이_취소되면_주문_헤더도_주문취소로_승격한다() {
        givenLine("지불완료");
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(0);

        service.cancelOne(7, 2, "단순변심", "", "admin", "관리자");

        ArgumentCaptor<Order> header = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).updateOrderCancel(header.capture());
        assertThat(header.getValue().getOrderNo()).isEqualTo(7);
        assertThat(header.getValue().getOrderState()).isEqualTo("주문취소");
    }
}
