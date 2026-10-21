package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.ship.mapper.ShipDirectMapper;
import com.shopwizard.ship.model.ShipDirectCheckRequest;
import com.shopwizard.ship.model.ShipDirectIssueRequest;
import com.shopwizard.ship.model.ShipDirectIssueResult;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.service.OrderLineLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 송장입력(발주확인 → 배송시작)과 출고지시/발주확인/배송완료 배치 집계. */
@ExtendWith(MockitoExtension.class)
class ShipDirectServiceTest {

    @Mock private ShipDirectMapper shipDirectMapper;
    @Mock private OrderLineLock lineLock;
    @Mock private OrderProdMapper orderProdMapper;
    @Mock private ShipDirectIssueService issueService;
    @Mock private ShipDirectCheckService checkService;
    @Mock private ShipDirectCompleteService completeService;
    @InjectMocks private ShipDirectService service;

    private static OrderProd lineIn(String state) {
        OrderProd op = new OrderProd();
        op.setOrderState(state);
        return op;
    }

    private static Map<String, Object> invoice(String deliCompName, String invNo) {
        Map<String, Object> p = new HashMap<>();
        p.put("orderNo", 7);
        p.put("orderProdNo", 2);
        p.put("orderChangeNo", 1);
        p.put("deliCompName", deliCompName);
        p.put("invNo", invNo);
        p.put("orderState", "주문취소");        // 클라이언트가 보낸 값은 무시되어야 한다
        p.put("changeId", "admin");
        p.put("changeName", "관리자");
        return p;
    }

    @Test
    void 송장입력은_클라이언트가_보낸_상태와_무관하게_배송시작으로_기록한다() {
        when(lineLock.lockLine(7, 2)).thenReturn(lineIn("발주확인"));
        when(shipDirectMapper.updateInvoice(any())).thenReturn(1);

        assertThat(service.updateInvoice(invoice("대한통운", "  12345  "))).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> ship = ArgumentCaptor.forClass(Map.class);
        verify(shipDirectMapper).updateInvoice(ship.capture());
        assertThat(ship.getValue()).containsEntry("orderState", "배송시작").containsEntry("invNo", "12345")
                .containsEntry("deliCompName", "대한통운");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> line = ArgumentCaptor.forClass(Map.class);
        verify(orderProdMapper).updateOrderState(line.capture());
        assertThat(line.getValue()).containsEntry("orderState", "배송시작").containsEntry("orderProdNo", 2);
    }

    @Test
    void 송장입력_0행이면_주문라인_상태를_바꾸지_않는다() {
        when(lineLock.lockLine(7, 2)).thenReturn(lineIn("발주확인"));
        when(shipDirectMapper.updateInvoice(any())).thenReturn(0);

        assertThat(service.updateInvoice(invoice("대한통운", "12345"))).isZero();

        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 송장입력은_발주확인이_아닌_라인이면_배송_테이블과_라인을_건드리지_않고_0을_돌려준다() {
        // 취소/환불이 먼저 커밋돼 라인이 주문취소가 됐다 — 송장입력이 그 라인을 배송시작으로 되돌리면 안 된다
        when(lineLock.lockLine(7, 2)).thenReturn(lineIn("주문취소"));

        assertThat(service.updateInvoice(invoice("대한통운", "12345"))).isZero();

        verifyNoInteractions(shipDirectMapper, orderProdMapper);
    }

    @Test
    void 택배사나_송장번호가_비어있으면_400이고_아무것도_하지_않는다() {
        for (Map<String, Object> bad : List.of(invoice("", "12345"), invoice("대한통운", "   "), invoice(null, null))) {
            assertThatThrownBy(() -> service.updateInvoice(bad))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        verifyNoInteractions(shipDirectMapper, orderProdMapper, lineLock);
    }

    @Test
    void 출고지시_배치는_건별_성공_실패_예외를_집계한다() {
        when(issueService.issueOne(eq(1), eq(1), anyString(), anyString())).thenReturn(null);
        when(issueService.issueOne(eq(2), eq(1), anyString(), anyString())).thenReturn("발주단가매칭이 필요합니다.");
        when(issueService.issueOne(eq(3), eq(1), anyString(), anyString())).thenThrow(new IllegalStateException("DB 오류"));

        ShipDirectIssueRequest req = new ShipDirectIssueRequest();
        req.setRegistId("admin");
        req.setRegistName("관리자");
        req.setLines(List.of(issueLine(1), issueLine(2), issueLine(3)));

        ShipDirectIssueResult result = service.issueBatch(req);

        assertThat(result.getSuccess()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(2);
        assertThat(result.getFailDetails()).containsExactly("2-1 : 발주단가매칭이 필요합니다.", "3-1 : DB 오류");
    }

    @Test
    void 발주확인_배송완료_배치도_한_건이_실패해도_나머지를_계속_처리한다() {
        when(checkService.checkOne(eq(1), eq(1), eq(1), anyString(), anyString())).thenReturn("출고지시 상태가 아니거나");
        when(checkService.checkOne(eq(2), eq(1), eq(1), anyString(), anyString())).thenReturn(null);
        when(completeService.completeOne(eq(1), eq(1), eq(1), anyString(), anyString())).thenReturn(null);
        when(completeService.completeOne(eq(2), eq(1), eq(1), anyString(), anyString())).thenThrow(new RuntimeException("오류"));

        ShipDirectCheckRequest req = new ShipDirectCheckRequest();
        req.setRegistId("admin");
        req.setRegistName("관리자");
        req.setLines(List.of(checkLine(1), checkLine(2)));

        ShipDirectIssueResult check = service.checkBatch(req);
        assertThat(check.getSuccess()).isEqualTo(1);
        assertThat(check.getFailed()).isEqualTo(1);

        ShipDirectIssueResult complete = service.completeBatch(req);
        assertThat(complete.getSuccess()).isEqualTo(1);
        assertThat(complete.getFailed()).isEqualTo(1);
        assertThat(complete.getFailDetails().get(0)).isEqualTo("2-1 : 오류");
    }

    @Test
    void 선택한_라인이_없으면_빈_결과를_돌려준다() {
        assertThat(service.issueBatch(new ShipDirectIssueRequest()).getSuccess()).isZero();
        assertThat(service.checkBatch(new ShipDirectCheckRequest()).getFailed()).isZero();
        assertThat(service.completeBatch(new ShipDirectCheckRequest()).getFailed()).isZero();
        verifyNoInteractions(issueService, checkService, completeService);
    }

    private static ShipDirectIssueRequest.Line issueLine(int orderNo) {
        ShipDirectIssueRequest.Line l = new ShipDirectIssueRequest.Line();
        l.setOrderNo(orderNo);
        l.setOrderProdNo(1);
        return l;
    }

    private static ShipDirectCheckRequest.Line checkLine(int orderNo) {
        ShipDirectCheckRequest.Line l = new ShipDirectCheckRequest.Line();
        l.setOrderNo(orderNo);
        l.setOrderProdNo(1);
        l.setOrderChangeNo(1);
        return l;
    }
}
