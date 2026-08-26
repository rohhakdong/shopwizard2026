package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderNoSeqMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderNoSeq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderMapper orderMapper;
    private final OrderNoSeqMapper orderNoSeqMapper;

    public List<Order> selectList(Map<String, Object> params) { return orderMapper.selectList(params); }
    public List<Order> selectListOrderList(Map<String, Object> params) { return orderMapper.selectListOrderList(params); }
    public int selectCountOrderList(Map<String, Object> params) { return orderMapper.selectCountOrderList(params); }
    public List<Order> selectListPay(Map<String, Object> params) { return orderMapper.selectListPay(params); }
    public Order select(Map<String, Object> params) { return orderMapper.select(params); }

    /**
     * OrderNo는 tOrdOrder의 auto_increment가 아니라 tOrdOrderNoSeq로 별도 채번해야 하는 값이라
     * (자세한 내용은 CheckoutService 참고), 호출자가 orderNo를 미리 채워 넣지 않은 경우 여기서
     * 대신 채번해준다. CheckoutService처럼 다른 테이블에도 같은 OrderNo를 써야 해서 미리
     * 채번해 넘기는 호출자는 그대로 그 값을 쓰고, 이 메서드는 재채번하지 않는다.
     */
    public void insert(Order order) {
        if (order.getOrderNo() == null) {
            OrderNoSeq seq = new OrderNoSeq();
            orderNoSeqMapper.insert(seq);
            order.setOrderNo(seq.getSeq());
        }
        orderMapper.insert(order);
    }
    public void updateOrderCancel(Order order) { orderMapper.updateOrderCancel(order); }
    public void updateOrderState(Map<String, Object> params) { orderMapper.updateOrderState(params); }
    public void updateRefundCancel(Map<String, Object> params) { orderMapper.updateRefundCancel(params); }
}
