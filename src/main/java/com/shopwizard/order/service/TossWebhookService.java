package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderPayLogTossMapper;
import com.shopwizard.order.mapper.OrderPayMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.order.model.OrderPayLogToss;
import com.shopwizard.payment.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 토스페이먼츠 웹훅(비동기 통지) 처리.
 * - 가상계좌 입금완료(status DONE): 결제 대기 중이던 주문을 "지불완료"로 갱신
 * - 결제취소/환불(status CANCELED): 관리자가 토스 대시보드에서 직접 취소한 경우에도
 *   우리 쪽 주문 상태를 "주문취소"로 동기화
 * - 그 외(PARTIAL_CANCELED 등): 라인별 부분환불 금액 처리 로직이 없어 상태는 바꾸지 않고
 *   감사 로그만 남긴다 (필요해지면 반품/부분환불 워크플로우와 함께 별도 구현 필요)
 *
 * 웹훅 바디는 위변조될 수 있으므로 그대로 신뢰하지 않고, paymentKey만 꺼내 토스 결제
 * 조회 API(TossPaymentService.getPayment)로 실제 상태를 다시 확인한 뒤 반영한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class TossWebhookService {

    private static final String SYSTEM_ID = "SYSTEM";
    private static final String SYSTEM_NAME = "토스웹훅";

    private final TossPaymentService tossPaymentService;
    private final OrderPayMapper orderPayMapper;
    private final OrderPayLogTossMapper orderPayLogTossMapper;
    private final OrderService orderService;
    private final OrderProdService orderProdService;

    public void handlePaymentStatusChanged(String paymentKey) throws Exception {
        if (paymentKey == null || paymentKey.isBlank()) return;

        Map<String, Object> payment = tossPaymentService.getPayment(paymentKey);
        String status = String.valueOf(payment.get("status"));

        OrderPay pay = orderPayMapper.selectByPgTradeNo(paymentKey);
        if (pay == null || pay.getOrderNo() == null) {
            return; // 이 시스템에서 생성한 결제가 아니거나 아직 저장 전 — 무시
        }
        int orderNo = pay.getOrderNo();

        Order order = orderService.select(Map.of("orderNo", orderNo));
        if (order == null) return;

        switch (status) {
            case "DONE" -> handleDone(orderNo, order, payment);
            case "CANCELED" -> handleCanceled(orderNo, order, payment);
            default -> orderPayLogTossMapper.insert(logFor(orderNo, payment)); // PARTIAL_CANCELED 등: 로그만 기록
        }
    }

    /** 가상계좌 입금완료 등으로 결제가 최종 완료됐을 때 — 멱등 처리 (이미 지불완료면 무시) */
    private void handleDone(int orderNo, Order order, Map<String, Object> payment) {
        if ("지불완료".equals(order.getOrderState())) return;

        Map<String, Object> params = new HashMap<>();
        params.put("orderNo", orderNo);
        params.put("orderState", "지불완료");
        params.put("changeId", SYSTEM_ID);
        params.put("changeName", SYSTEM_NAME);
        orderService.updateOrderState(params);
        orderProdService.updateOrderState(params);

        orderPayLogTossMapper.insert(logFor(orderNo, payment));
    }

    /** 관리자가 토스 대시보드에서 직접 결제를 취소한 경우 — 멱등 처리 (이미 주문취소면 무시) */
    private void handleCanceled(int orderNo, Order order, Map<String, Object> payment) {
        if ("주문취소".equals(order.getOrderState())) return;

        Order cancelOrder = new Order();
        cancelOrder.setOrderNo(orderNo);
        cancelOrder.setOrderState("주문취소");
        cancelOrder.setChangeId(SYSTEM_ID);
        cancelOrder.setChangeName(SYSTEM_NAME);
        orderService.updateOrderCancel(cancelOrder);

        for (Integer orderProdNo : orderProdService.selectOrderProdNoList(orderNo)) {
            Map<String, Object> params = new HashMap<>();
            params.put("orderNo", orderNo);
            params.put("orderProdNo", orderProdNo);
            params.put("orderState", "주문취소");
            params.put("changeId", SYSTEM_ID);
            params.put("changeName", SYSTEM_NAME);
            orderProdService.updateOrderState(params);
        }

        orderPayLogTossMapper.insert(logFor(orderNo, payment));
    }

    private static OrderPayLogToss logFor(int orderNo, Map<String, Object> payment) {
        return OrderPayLogToss.from(orderNo, payment, SYSTEM_ID, SYSTEM_NAME);
    }
}
