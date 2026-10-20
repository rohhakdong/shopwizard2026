package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderProdMapper;
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
import static org.mockito.Mockito.when;

/**
 * 발주확인(출고지시 → 발주확인), 배송완료(배송시작 → 배송완료) 1건 처리.
 * 소스 상태 가드는 ShipDirectMapper.update 의 WHERE 절이 맡으므로, 서비스는 "0행이면 라인 상태를
 * 건드리지 않고 사유를 돌려준다"는 계약을 지켜야 한다.
 */
@ExtendWith(MockitoExtension.class)
class ShipDirectCheckCompleteServiceTest {

    @Mock private ShipDirectMapper shipDirectMapper;
    @Mock private OrderProdMapper orderProdMapper;

    @SuppressWarnings("unchecked")
    private Map<String, Object> capture(boolean ship) {
        ArgumentCaptor<Map<String, Object>> c = ArgumentCaptor.forClass(Map.class);
        if (ship) verify(shipDirectMapper).update(c.capture());
        else verify(orderProdMapper).updateOrderState(c.capture());
        return c.getValue();
    }

    @Test
    void 발주확인_성공하면_출고행과_주문라인_상태를_모두_발주확인으로_맞춘다() {
        ShipDirectCheckService service = new ShipDirectCheckService(shipDirectMapper, orderProdMapper);
        when(shipDirectMapper.update(any())).thenReturn(1);

        assertThat(service.checkOne(5, 2, 1, "admin", "관리자")).isNull();

        assertThat(capture(true)).containsEntry("orderState", "발주확인")
                .containsEntry("orderNo", 5).containsEntry("orderProdNo", 2).containsEntry("orderChangeNo", 1);
        assertThat(capture(false)).containsEntry("orderState", "발주확인").containsEntry("orderProdNo", 2);
    }

    @Test
    void 발주확인_0행이면_라인_상태를_건드리지_않는다() {
        ShipDirectCheckService service = new ShipDirectCheckService(shipDirectMapper, orderProdMapper);
        when(shipDirectMapper.update(any())).thenReturn(0);

        assertThat(service.checkOne(5, 2, 1, "admin", "관리자")).contains("출고지시 상태가 아니거나");

        verify(orderProdMapper, never()).updateOrderState(any());
    }

    @Test
    void 배송완료_성공하면_출고행과_주문라인_상태를_모두_배송완료로_맞춘다() {
        ShipDirectCompleteService service = new ShipDirectCompleteService(shipDirectMapper, orderProdMapper);
        when(shipDirectMapper.update(any())).thenReturn(1);

        assertThat(service.completeOne(5, 2, 1, "admin", "관리자")).isNull();

        assertThat(capture(true)).containsEntry("orderState", "배송완료");
        assertThat(capture(false)).containsEntry("orderState", "배송완료");
    }

    @Test
    void 배송완료_0행이면_라인_상태를_건드리지_않는다() {
        ShipDirectCompleteService service = new ShipDirectCompleteService(shipDirectMapper, orderProdMapper);
        when(shipDirectMapper.update(any())).thenReturn(0);

        assertThat(service.completeOne(5, 2, 1, "admin", "관리자")).contains("배송시작 상태가 아니거나");

        verify(orderProdMapper, never()).updateOrderState(any());
    }
}
