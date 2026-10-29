package com.shopwizard.order.service;

import com.shopwizard.order.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * PG 의 비동기 결과(입금 통보·미입금 만료·PG 쪽 취소)를 주문 상태에 반영하는 공용 로직 — 토스 웹훅, KCP 웹훅, KCP 가상계좌 만료 잡이 함께 쓴다.
 * 호출자는 먼저 {@link OrderLineLock#lockHeader} 로 헤더를 잠근 뒤 주문 상태를 읽고 이 메서드를 부른다(취소/환불확정과 같은 잠금 순서).
 * 트랜잭션은 호출자의 것을 이어 받는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class OrderPaymentSyncService {

    private final OrderService orderService;
    private final OrderProdService orderProdService;
    private final StockLedgerService stockLedgerService;   // 취소된 라인의 재고 복원(차감 이력이 있는 라인만, 1회)

    /**
     * 입금 대기 중이던("주문접수") 주문을 "지불완료"로 바꾼다. 이미 지불완료거나 취소·환불 단계인 주문은 건드리지 않는다.
     *
     * @return 실제로 바꿨으면 true
     */
    public boolean markPaid(int orderNo, Order order, String actorId, String actorName) {
        if (order == null || !"주문접수".equals(order.getOrderState())) return false;

        Map<String, Object> params = new HashMap<>();
        params.put("orderNo", orderNo);
        params.put("orderState", "지불완료");
        params.put("changeId", actorId);
        params.put("changeName", actorName);
        orderService.updateOrderState(params);
        orderProdService.updateOrderState(params);
        return true;
    }

    /**
     * 배송 전 라인만 주문취소로 바꾸고(조건부 SQL) 재고를 되돌린다. 반품/환불 단계의 라인은 건드리지 않으며, 헤더는
     * 모든 라인이 주문취소가 됐을 때만 승격한다(방금 환불완료/반품완료로 만든 건을 되돌려 덮지 않도록).
     */
    public void cancelBeforeShipment(int orderNo, String actorId, String actorName) {
        for (Integer orderProdNo : orderProdService.selectOrderProdNoList(orderNo)) {
            Map<String, Object> params = new HashMap<>();
            params.put("orderNo", orderNo);
            params.put("orderProdNo", orderProdNo);
            params.put("changeId", actorId);
            params.put("changeName", actorName);
            // 실제로 주문취소로 바뀐 라인(배송 전)만 재고를 되돌린다 — 이미 반품/환불 단계인 라인은 그쪽 흐름에서 복원된다.
            if (orderProdService.cancelByWebhook(params) > 0) {
                stockLedgerService.restore(orderNo, orderProdNo, actorId);
            }
        }

        Map<String, Object> diff = new HashMap<>();
        diff.put("orderNo", orderNo);
        diff.put("orderState", "주문취소");
        if (orderProdService.countOrderStateDiffer(diff) == 0) {
            Order cancelOrder = new Order();
            cancelOrder.setOrderNo(orderNo);
            cancelOrder.setOrderState("주문취소");
            cancelOrder.setChangeId(actorId);
            cancelOrder.setChangeName(actorName);
            orderService.updateOrderCancel(cancelOrder);
        }
    }
}
