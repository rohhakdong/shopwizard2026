package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderProd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 주문라인 1건을 주문취소로 전환한다 (지불완료/출고지시/발주확인 → 주문취소).
 * {@code OrderProdMapper.updateReturnReason}/{@code updateOrderState}에는 원본 상태를 확인하는
 * WHERE 가드가 없으므로, 여기서 {@link OrderProdMapper#selectLineRaw}로 먼저 확인한다.
 * 그 주문의 모든 라인이 주문취소에 도달하면 {@code tOrdOrder} 헤더도 승격한다(레거시 캐스케이드,
 * {@link OrderProdMapper#countOrderStateDiffer}로 판단 — Toss 웹훅(TossWebhookService)이 이미
 * 쓰고 있던 패턴).
 *
 * <p>OrderService(오케스트레이션, cancelBatch)와 다른 빈으로 분리한 이유는 건별
 * {@code REQUIRES_NEW} 격리를 위해서다(출고지시의 ShipDirectIssueService 와 같은 패턴).</p>
 */
@Service
@RequiredArgsConstructor
public class OrderCancelService {

    private static final Set<String> CANCELABLE_STATES = Set.of("지불완료", "출고지시", "발주확인");

    private final OrderMapper orderMapper;
    private final OrderProdMapper orderProdMapper;
    private final StockLedgerService stockLedgerService;
    private final OrderLineLock lineLock;   // 상태 확인~갱신 사이에 다른 요청이 끼어들지 못하게 행 잠금

    /** 성공하면 null, 실패하면 사유 문자열을 반환한다(호출자가 건별 결과를 집계). */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String cancelOne(Integer orderNo, Integer orderProdNo, String reason, String reasonDetail,
                             String registId, String registName) {
        // 헤더 → 라인 순으로 잠근다(같은 주문의 라인을 동시에 취소해도 헤더 승격 판단이 서로의 결과를 본다).
        OrderProd op = lineLock.lockHeaderAndLine(orderNo, orderProdNo);
        if (op == null) {
            return "주문라인을 찾을 수 없습니다.";
        }
        if (!CANCELABLE_STATES.contains(op.getOrderState())) {
            return "취소 가능한 상태가 아닙니다. (현재 상태: " + op.getOrderState() + ")";
        }

        OrderProd upd = new OrderProd();
        upd.setOrderNo(orderNo);
        upd.setOrderProdNo(orderProdNo);
        upd.setOrderState("주문취소");
        upd.setShipCancelReciptDate("Y");
        upd.setReturnReason(reason);
        upd.setReturnReasonDetail(reasonDetail);
        upd.setChangeId(registId);
        upd.setChangeName(registName);
        orderProdMapper.updateReturnReason(upd);

        Map<String, Object> diff = new HashMap<>();
        diff.put("orderNo", orderNo);
        diff.put("orderState", "주문취소");
        if (orderProdMapper.countOrderStateDiffer(diff) == 0) {
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setOrderState("주문취소");
            order.setChangeId(registId);
            order.setChangeName(registName);
            orderMapper.updateOrderCancel(order);
        }

        // 배송 전 취소라 물건이 나가지 않았다 — 이 시스템이 차감한 이력이 있는 라인만 재고를 되돌린다.
        stockLedgerService.restore(orderNo, orderProdNo, registId);

        return null;
    }
}
