package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.service.StockLedgerService;
import com.shopwizard.ship.mapper.ReturnDirectMapper;
import com.shopwizard.ship.model.ReturnRequestBatchRequest;
import com.shopwizard.ship.model.ShipDirectIssueResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 반품확정(반품요청 → 반품완료) 캐스케이드와 반품요청 배치 집계. */
@ExtendWith(MockitoExtension.class)
class ReturnDirectServiceTest {

    @Mock private ReturnDirectMapper returnDirectMapper;
    @Mock private OrderProdMapper orderProdMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private ReturnRequestService returnRequestService;
    @Mock private StockLedgerService stockLedgerService;
    @InjectMocks private ReturnDirectService service;

    private static Map<String, Object> params() {
        Map<String, Object> p = new HashMap<>();
        p.put("orderNo", 7);
        p.put("orderProdNo", 2);
        p.put("orderChangeNo", 1);
        p.put("orderState", "엉뚱한상태");      // 클라이언트가 보낸 값은 무시되어야 한다
        p.put("changeId", "admin");
        p.put("changeName", "관리자");
        return p;
    }

    @Test
    void 반품요청_건이_아니라서_0행이면_라인과_헤더와_재고를_건드리지_않는다() {
        when(returnDirectMapper.update(any())).thenReturn(0);

        assertThat(service.update(params())).isZero();

        verifyNoInteractions(orderMapper, stockLedgerService);
        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 성공하면_라인을_반품완료로_바꾸고_재고를_복원한다_클라이언트_상태는_쓰지_않는다() {
        when(returnDirectMapper.update(any())).thenReturn(1);
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(1);

        assertThat(service.update(params())).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> line = ArgumentCaptor.forClass(Map.class);
        verify(orderProdMapper).updateOrderState(line.capture());
        assertThat(line.getValue()).containsEntry("orderState", "반품완료").containsEntry("orderProdNo", 2);
        verify(stockLedgerService).restore(7, 2, "admin");
        verify(orderMapper, never()).updateOrderState(any());   // 다른 라인이 아직 반품완료가 아니다
    }

    @Test
    void 모든_라인이_반품완료가_되면_주문_헤더도_승격한다() {
        when(returnDirectMapper.update(any())).thenReturn(1);
        when(orderProdMapper.countOrderStateDiffer(any())).thenReturn(0);

        service.update(params());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> header = ArgumentCaptor.forClass(Map.class);
        verify(orderMapper).updateOrderState(header.capture());
        assertThat(header.getValue()).containsEntry("orderState", "반품완료").containsEntry("orderNo", 7);
    }

    @Test
    void 반품요청_배치는_건별_결과를_성공_실패로_집계하고_예외도_실패로_센다() {
        when(returnRequestService.requestOne(eq(1), eq(1), anyString(), anyString(), anyString(), anyString())).thenReturn(null);
        when(returnRequestService.requestOne(eq(2), eq(1), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("배송완료 상태가 아닙니다. (현재 상태: 지불완료)");
        when(returnRequestService.requestOne(eq(3), eq(1), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("DB 오류"));

        ReturnRequestBatchRequest req = new ReturnRequestBatchRequest();
        req.setReturnReason("단순변심");
        req.setReturnReasonDetail("");
        req.setRegistId("admin");
        req.setRegistName("관리자");
        req.setLines(List.of(line(1), line(2), line(3)));

        ShipDirectIssueResult result = service.requestBatch(req);

        assertThat(result.getSuccess()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(2);
        assertThat(result.getFailDetails()).hasSize(2);
        assertThat(result.getFailDetails().get(0)).startsWith("2-1 :").contains("배송완료 상태가 아닙니다");
        assertThat(result.getFailDetails().get(1)).startsWith("3-1 :").contains("DB 오류");
    }

    private static ReturnRequestBatchRequest.Line line(int orderNo) {
        ReturnRequestBatchRequest.Line l = new ReturnRequestBatchRequest.Line();
        l.setOrderNo(orderNo);
        l.setOrderProdNo(1);
        return l;
    }
}
