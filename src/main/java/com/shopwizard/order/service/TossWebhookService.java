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
import java.util.Set;

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
    /** 이미 취소/환불까지 끝난 주문 — CANCELED 웹훅을 다시 받아도 아무것도 바꾸지 않는다. */
    private static final Set<String> TERMINAL_CANCEL_STATES = Set.of("주문취소", "환불완료");

    private final TossPaymentService tossPaymentService;
    private final OrderPayMapper orderPayMapper;
    private final OrderPayLogTossMapper orderPayLogTossMapper;
    private final OrderService orderService;
    private final OrderProdService orderProdService;
    private final OrderLineLock lineLock;   // 헤더 → 라인 순서의 행 잠금(취소/환불확정과 같은 규칙)
    private final OrderPaymentSyncService paymentSync;   // 배송 전 라인 취소 + 재고 복원 + 헤더 승격(KCP 웹훅·만료 잡과 공용)

    public void handlePaymentStatusChanged(String paymentKey) throws Exception {
        if (paymentKey == null || paymentKey.isBlank()) return;

        Map<String, Object> payment = tossPaymentService.getPayment(paymentKey);
        String status = String.valueOf(payment.get("status"));

        OrderPay pay = orderPayMapper.selectByPgTradeNo(paymentKey);
        if (pay == null || pay.getOrderNo() == null) {
            return; // 이 시스템에서 생성한 결제가 아니거나 아직 저장 전 — 무시
        }
        int orderNo = pay.getOrderNo();

        // 헤더를 먼저 잠근 뒤 주문 상태를 읽는다 — 같은 주문의 취소/환불확정과 직렬화되고, 잠금 순서(헤더→라인)도 같다.
        lineLock.lockHeader(orderNo);
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

    /**
     * 결제가 취소됐을 때(토스 대시보드 취소, 또는 환불확정이 호출한 토스 취소의 되돌림 통지) — 멱등 처리.
     *
     * <p>이미 주문취소/환불완료인 주문은 건너뛴다. 환불확정이 토스 취소를 호출하면 토스가 곧바로 이 웹훅을
     * 보내는데, 예전처럼 헤더+모든 라인을 무조건 주문취소로 덮어쓰면 방금 환불완료(또는 반품완료)로 만든
     * 건이 주문취소로 되돌아간다. 그래서 라인은 <b>배송 전 상태인 것만</b> 주문취소로 바꾸고(조건부 SQL),
     * 반품/환불 단계의 라인은 건드리지 않으며, 헤더는 모든 라인이 주문취소가 됐을 때만 승격한다.
     * 배송이 이미 시작된 주문을 대시보드에서 취소한 경우는 상태를 바꾸지 않고 결제 로그만 남긴다
     * (반품/환불 워크플로우로 처리해야 하는 건).</p>
     */
    private void handleCanceled(int orderNo, Order order, Map<String, Object> payment) {
        if (TERMINAL_CANCEL_STATES.contains(order.getOrderState())) return;

        paymentSync.cancelBeforeShipment(orderNo, SYSTEM_ID, SYSTEM_NAME);

        orderPayLogTossMapper.insert(logFor(orderNo, payment));
    }

    private static OrderPayLogToss logFor(int orderNo, Map<String, Object> payment) {
        return OrderPayLogToss.from(orderNo, payment, SYSTEM_ID, SYSTEM_NAME);
    }
}
