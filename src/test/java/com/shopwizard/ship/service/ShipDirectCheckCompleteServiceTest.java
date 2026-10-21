package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.service.OrderLineLock;
import com.shopwizard.ship.mapper.ShipDirectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 발주확인(출고지시 → 발주확인), 배송완료(배송시작 → 배송완료) 1건 처리.
 * 라인을 먼저 잠그고 원본 라인 상태가 기대 상태인지 확인한다 — 배송 테이블만 보면 이미 주문취소·환불된 라인도 계속
 * 진행시키고 뒤따르는 무조건 UPDATE 가 취소 라인을 되살린다. 그 다음 ShipDirectMapper.update 의 WHERE 가드가 한 번 더
 * 걸러주므로, 서비스는 "0행이면 라인 상태를 건드리지 않고 사유를 돌려준다"는 계약도 지켜야 한다.
 */
@ExtendWith(MockitoExtension.class)
class ShipDirectCheckCompleteServiceTest {

    @Mock private ShipDirectMapper shipDirectMapper;
    @Mock private OrderProdMapper orderProdMapper;
    @Mock private OrderLineLock lineLock;

    private static OrderProd line(String state) {
        OrderProd op = new OrderProd();
        op.setOrderState(state);
        return op;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capture(boolean ship) {
        ArgumentCaptor<Map<String, Object>> c = ArgumentCaptor.forClass(Map.class);
        if (ship) verify(shipDirectMapper).update(c.capture());
        else verify(orderProdMapper).updateOrderState(c.capture());
        return c.getValue();
    }

    @Test
    void 발주확인_성공하면_출고행과_주문라인_상태를_모두_발주확인으로_맞춘다() {
        ShipDirectCheckService service = new ShipDirectCheckService(shipDirectMapper, orderProdMapper, lineLock);
        when(lineLock.lockLine(5, 2)).thenReturn(line("출고지시"));
        when(shipDirectMapper.update(any())).thenReturn(1);

        assertThat(service.checkOne(5, 2, 1, "admin", "관리자")).isNull();

        assertThat(capture(true)).containsEntry("orderState", "발주확인")
                .containsEntry("orderNo", 5).containsEntry("orderProdNo", 2).containsEntry("orderChangeNo", 1);
        assertThat(capture(false)).containsEntry("orderState", "발주확인").containsEntry("orderProdNo", 2);
    }

    @Test
    void 발주확인_0행이면_라인_상태를_건드리지_않는다() {
        ShipDirectCheckService service = new ShipDirectCheckService(shipDirectMapper, orderProdMapper, lineLock);
        when(lineLock.lockLine(5, 2)).thenReturn(line("출고지시"));
        when(shipDirectMapper.update(any())).thenReturn(0);

        assertThat(service.checkOne(5, 2, 1, "admin", "관리자")).contains("출고지시 상태가 아니거나");

        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 발주확인_이미_주문취소된_라인은_되살리지_않고_아무것도_쓰지_않는다() {
        ShipDirectCheckService service = new ShipDirectCheckService(shipDirectMapper, orderProdMapper, lineLock);
        when(lineLock.lockLine(5, 2)).thenReturn(line("주문취소"));   // 취소가 먼저 커밋됨(배송 테이블 행은 출고지시 그대로)

        assertThat(service.checkOne(5, 2, 1, "admin", "관리자")).contains("현재 상태: 주문취소");

        verifyNoInteractions(shipDirectMapper);
        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 발주확인_라인이_없으면_사유를_돌려준다() {
        ShipDirectCheckService service = new ShipDirectCheckService(shipDirectMapper, orderProdMapper, lineLock);
        when(lineLock.lockLine(5, 2)).thenReturn(null);

        assertThat(service.checkOne(5, 2, 1, "admin", "관리자")).contains("찾을 수 없습니다");
        verifyNoInteractions(shipDirectMapper);
    }

    @Test
    void 배송완료_성공하면_출고행과_주문라인_상태를_모두_배송완료로_맞춘다() {
        ShipDirectCompleteService service = new ShipDirectCompleteService(shipDirectMapper, orderProdMapper, lineLock);
        when(lineLock.lockLine(5, 2)).thenReturn(line("배송시작"));
        when(shipDirectMapper.update(any())).thenReturn(1);

        assertThat(service.completeOne(5, 2, 1, "admin", "관리자")).isNull();

        assertThat(capture(true)).containsEntry("orderState", "배송완료");
        assertThat(capture(false)).containsEntry("orderState", "배송완료");
    }

    @Test
    void 배송완료_0행이면_라인_상태를_건드리지_않는다() {
        ShipDirectCompleteService service = new ShipDirectCompleteService(shipDirectMapper, orderProdMapper, lineLock);
        when(lineLock.lockLine(5, 2)).thenReturn(line("배송시작"));
        when(shipDirectMapper.update(any())).thenReturn(0);

        assertThat(service.completeOne(5, 2, 1, "admin", "관리자")).contains("배송시작 상태가 아니거나");

        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 배송완료_이미_환불완료된_라인은_되살리지_않는다() {
        ShipDirectCompleteService service = new ShipDirectCompleteService(shipDirectMapper, orderProdMapper, lineLock);
        when(lineLock.lockLine(5, 2)).thenReturn(line("환불완료"));

        assertThat(service.completeOne(5, 2, 1, "admin", "관리자")).contains("현재 상태: 환불완료");

        verifyNoInteractions(shipDirectMapper);
        verify(orderProdMapper, never()).updateOrderState(any());
    }
}
