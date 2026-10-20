package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.ship.mapper.ReturnDirectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 반품요청(배송완료 → 반품요청) 1건 처리. */
@ExtendWith(MockitoExtension.class)
class ReturnRequestServiceTest {

    @Mock private ReturnDirectMapper returnDirectMapper;
    @Mock private OrderProdMapper orderProdMapper;
    @Mock private OrderMapper orderMapper;
    @InjectMocks private ReturnRequestService service;

    private void givenLine(String state) {
        OrderProd op = new OrderProd();
        op.setOrderState(state);
        when(orderProdMapper.selectLineRaw(any())).thenReturn(op);
    }

    @Test
    void 주문라인이_없으면_아무것도_바꾸지_않는다() {
        when(orderProdMapper.selectLineRaw(any())).thenReturn(null);

        assertThat(service.requestOne(1, 1, "단순변심", "", "admin", "관리자")).contains("찾을 수 없습니다");

        verifyNoInteractions(returnDirectMapper, orderMapper);
        verify(orderProdMapper, never()).updateReturnReason(any());
    }

    @Test
    void 배송완료가_아니면_거부한다() {
        for (String state : new String[]{"지불완료", "출고지시", "발주확인", "배송시작", "반품요청", "반품완료", "주문취소", "환불완료"}) {
            givenLine(state);
            assertThat(service.requestOne(1, 1, "단순변심", "", "admin", "관리자")).as(state)
                    .contains("배송완료 상태가 아닙니다").contains(state);
        }
        verifyNoInteractions(returnDirectMapper, orderMapper);
        verify(orderProdMapper, never()).updateReturnReason(any());
        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 사유를_먼저_기록한_뒤_반품행을_만들고_그_다음에_라인_상태를_반품요청으로_바꾼다() {
        givenLine("배송완료");
        when(returnDirectMapper.insertReturnDirect(any())).thenReturn(1);
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1);

        assertThat(service.requestOne(7, 2, "품질불량", "파손", "admin", "관리자")).isNull();

        // INSERT 가 라인의 ReturnReason 을 복사해가고 배송완료 가드가 걸려 있어서 순서가 곧 정합성이다.
        InOrder order = inOrder(orderProdMapper, returnDirectMapper);
        ArgumentCaptor<OrderProd> reason = ArgumentCaptor.forClass(OrderProd.class);
        order.verify(orderProdMapper).updateReturnReason(reason.capture());
        order.verify(returnDirectMapper).insertReturnDirect(any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> state = ArgumentCaptor.forClass(Map.class);
        order.verify(orderProdMapper).updateOrderState(state.capture());

        assertThat(reason.getValue().getReturnReason()).isEqualTo("품질불량");
        assertThat(reason.getValue().getReturnReasonDetail()).isEqualTo("파손");
        assertThat(reason.getValue().getOrderState()).isNull();            // 사유 기록 시점엔 상태를 바꾸지 않는다
        assertThat(state.getValue()).containsEntry("orderState", "반품요청").containsEntry("orderProdNo", 2);
    }

    @Test
    void 반품행_생성이_0행이면_라인_상태를_바꾸지_않고_사유를_돌려준다() {
        givenLine("배송완료");
        when(returnDirectMapper.insertReturnDirect(any())).thenReturn(0);

        assertThat(service.requestOne(7, 2, "단순변심", "", "admin", "관리자")).contains("이미 반품요청");

        verify(orderProdMapper, never()).updateOrderState(any());
        verify(orderMapper, never()).updateOrderState(any());
    }

    @Test
    void 다른_라인이_배송완료로_남아있으면_헤더는_승격하지_않고_모두_반품요청이면_승격한다() {
        givenLine("배송완료");
        when(returnDirectMapper.insertReturnDirect(any())).thenReturn(1);
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1, 0);

        service.requestOne(7, 1, "단순변심", "", "admin", "관리자");
        verify(orderMapper, never()).updateOrderState(any());

        service.requestOne(7, 2, "단순변심", "", "admin", "관리자");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> header = ArgumentCaptor.forClass(Map.class);
        verify(orderMapper).updateOrderState(header.capture());
        assertThat(header.getValue()).containsEntry("orderState", "반품요청").containsEntry("orderNo", 7);
    }
}
