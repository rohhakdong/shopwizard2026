package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderNoSeqMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderNoSeq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * OrderService.insert(Order)의 OrderNo 채번 안전장치 검증.
 * (범용 POST /order/order가 OrderNo 채번 없이 깨져 있던 문제 — CheckoutService처럼
 * 이미 orderNo를 채번해서 넘기는 호출자는 건드리지 않으면서, 그렇지 않은 호출자는
 * 여기서 대신 채번해준다.)
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderNoSeqMapper orderNoSeqMapper;

    @InjectMocks private OrderService orderService;

    @Test
    void orderNo가_없으면_직접_채번해서_채운다() {
        doAnswer(inv -> {
            OrderNoSeq seq = inv.getArgument(0);
            seq.setSeq(555001);
            return null;
        }).when(orderNoSeqMapper).insert(any());

        Order order = new Order();
        order.setOrderName("테스트");

        orderService.insert(order);

        assertThat(order.getOrderNo()).isEqualTo(555001);
        verify(orderMapper).insert(order);
    }

    @Test
    void orderNo가_이미_있으면_재채번하지_않는다() {
        Order order = new Order();
        order.setOrderNo(2414700);
        order.setOrderName("테스트");

        orderService.insert(order);

        verify(orderNoSeqMapper, never()).insert(any());
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        assertThat(captor.getValue().getOrderNo()).isEqualTo(2414700);
    }
}
