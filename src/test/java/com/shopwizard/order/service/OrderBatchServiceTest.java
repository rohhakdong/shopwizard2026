package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderNoSeqMapper;
import com.shopwizard.order.model.OrderCancelRequest;
import com.shopwizard.order.model.RefundConfirmRequest;
import com.shopwizard.ship.model.ShipDirectIssueResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 주문취소접수/환불확정 배치 오케스트레이터 — 한 건의 실패(예외 포함)가 나머지 건을 막지 않는다. */
@ExtendWith(MockitoExtension.class)
class OrderBatchServiceTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderNoSeqMapper orderNoSeqMapper;
    @Mock private OrderCancelService orderCancelService;
    @Mock private RefundConfirmService refundConfirmService;
    @InjectMocks private OrderService service;

    @Test
    void 주문취소_배치는_건별로_집계한다() {
        when(orderCancelService.cancelOne(eq(1), eq(1), anyString(), anyString(), anyString(), anyString())).thenReturn(null);
        when(orderCancelService.cancelOne(eq(2), eq(1), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("취소 가능한 상태가 아닙니다. (현재 상태: 배송완료)");
        when(orderCancelService.cancelOne(eq(3), eq(1), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("DB 오류"));

        OrderCancelRequest req = new OrderCancelRequest();
        req.setReason("단순변심");
        req.setReasonDetail("");
        req.setRegistId("admin");
        req.setRegistName("관리자");
        req.setLines(List.of(cancelLine(1), cancelLine(2), cancelLine(3)));

        ShipDirectIssueResult result = service.cancelBatch(req);

        assertThat(result.getSuccess()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(2);
        assertThat(result.getFailDetails()).hasSize(2);
        assertThat(result.getFailDetails().get(0)).startsWith("2-1 :").contains("배송완료");
        assertThat(result.getFailDetails().get(1)).isEqualTo("3-1 : DB 오류");
    }

    @Test
    void 환불확정_배치는_토스_실패한_건만_실패로_세고_나머지는_진행한다() throws Exception {
        when(refundConfirmService.confirmOne(eq(1), eq(1), anyBoolean(), anyString(), anyString())).thenReturn(null);
        when(refundConfirmService.confirmOne(eq(2), eq(1), anyBoolean(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("토스 결제 취소 실패: 잔액 부족"));
        when(refundConfirmService.confirmOne(eq(3), eq(1), anyBoolean(), anyString(), anyString())).thenReturn(null);

        RefundConfirmRequest req = new RefundConfirmRequest();
        req.setRegistId("admin");
        req.setRegistName("관리자");
        req.setLines(List.of(refundLine(1), refundLine(2), refundLine(3)));

        ShipDirectIssueResult result = service.confirmRefundBatch(req);

        assertThat(result.getSuccess()).isEqualTo(2);
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getFailDetails()).containsExactly("2-1 : 토스 결제 취소 실패: 잔액 부족");
    }

    @Test
    void 선택한_라인이_없으면_아무것도_호출하지_않는다() {
        assertThat(service.cancelBatch(new OrderCancelRequest()).getFailed()).isZero();
        assertThat(service.confirmRefundBatch(new RefundConfirmRequest()).getFailed()).isZero();
        verifyNoInteractions(orderCancelService, refundConfirmService);
    }

    private static OrderCancelRequest.Line cancelLine(int orderNo) {
        OrderCancelRequest.Line l = new OrderCancelRequest.Line();
        l.setOrderNo(orderNo);
        l.setOrderProdNo(1);
        return l;
    }

    private static RefundConfirmRequest.Line refundLine(int orderNo) {
        RefundConfirmRequest.Line l = new RefundConfirmRequest.Line();
        l.setOrderNo(orderNo);
        l.setOrderProdNo(1);
        return l;
    }
}
