package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderNoSeqMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderCancelRequest;
import com.shopwizard.order.model.OrderNoSeq;
import com.shopwizard.ship.model.ShipDirectCheckRequest;
import com.shopwizard.ship.model.ShipDirectIssueResult;
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
    private final OrderCancelService orderCancelService;
    private final RefundConfirmService refundConfirmService;

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

    /**
     * 주문취소접수 화면 — 선택한 주문라인들을 건별로 주문취소 전환한다.
     * 건별로 {@link OrderCancelService#cancelOne}이 REQUIRES_NEW로 독립 커밋되므로,
     * 한 건이 실패해도 나머지 건 처리에는 영향이 없다.
     */
    public ShipDirectIssueResult cancelBatch(OrderCancelRequest req) {
        ShipDirectIssueResult result = new ShipDirectIssueResult();
        if (req.getLines() == null) return result;
        for (OrderCancelRequest.Line line : req.getLines()) {
            try {
                String err = orderCancelService.cancelOne(
                        line.getOrderNo(), line.getOrderProdNo(), req.getReason(), req.getReasonDetail(),
                        req.getRegistId(), req.getRegistName());
                if (err != null) {
                    result.setFailed(result.getFailed() + 1);
                    result.getFailDetails().add(line.getOrderNo() + "-" + line.getOrderProdNo() + " : " + err);
                } else {
                    result.setSuccess(result.getSuccess() + 1);
                }
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                result.getFailDetails().add(line.getOrderNo() + "-" + line.getOrderProdNo() + " : " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * 환불확정 화면 — 선택한 주문라인들(주문취소 또는 반품완료)을 건별로 환불완료 전환한다.
     * 건별로 {@link RefundConfirmService#confirmOne}이 REQUIRES_NEW로 독립 커밋되므로,
     * 한 건이 실패해도 나머지 건 처리에는 영향이 없다.
     */
    public ShipDirectIssueResult confirmRefundBatch(ShipDirectCheckRequest req) {
        ShipDirectIssueResult result = new ShipDirectIssueResult();
        if (req.getLines() == null) return result;
        for (ShipDirectCheckRequest.Line line : req.getLines()) {
            try {
                String err = refundConfirmService.confirmOne(
                        line.getOrderNo(), line.getOrderProdNo(), req.getRegistId(), req.getRegistName());
                if (err != null) {
                    result.setFailed(result.getFailed() + 1);
                    result.getFailDetails().add(line.getOrderNo() + "-" + line.getOrderProdNo() + " : " + err);
                } else {
                    result.setSuccess(result.getSuccess() + 1);
                }
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                result.getFailDetails().add(line.getOrderNo() + "-" + line.getOrderProdNo() + " : " + e.getMessage());
            }
        }
        return result;
    }
}
