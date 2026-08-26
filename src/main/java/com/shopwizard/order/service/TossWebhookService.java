package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderPayMapper;
import com.shopwizard.order.model.Order;
import com.shopwizard.order.model.OrderPay;
import com.shopwizard.payment.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 토스페이먼츠 웹훅(비동기 통지) 처리.
 * 가상계좌 입금완료(PAYMENT_STATUS_CHANGED → DONE)를 받아 결제 대기 중이던 주문을
 * "지불완료"로 갱신한다.
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
    private final OrderService orderService;
    private final OrderProdService orderProdService;

    public void handlePaymentStatusChanged(String paymentKey) throws Exception {
        if (paymentKey == null || paymentKey.isBlank()) return;

        Map<String, Object> payment = tossPaymentService.getPayment(paymentKey);
        if (!"DONE".equals(String.valueOf(payment.get("status")))) {
            return; // 아직 입금 전이거나 취소 등 다른 상태 — 이 웹훅에서는 처리하지 않음
        }

        OrderPay pay = orderPayMapper.selectByPgTradeNo(paymentKey);
        if (pay == null || pay.getOrderNo() == null) {
            return; // 이 시스템에서 생성한 결제가 아니거나 아직 저장 전 — 무시
        }

        Order order = orderService.select(Map.of("orderNo", pay.getOrderNo()));
        if (order == null || "지불완료".equals(order.getOrderState())) {
            return; // 이미 반영됨 — 웹훅 중복 전송에 대한 멱등 처리
        }

        Map<String, Object> params = new HashMap<>();
        params.put("orderNo", pay.getOrderNo());
        params.put("orderState", "지불완료");
        params.put("changeId", SYSTEM_ID);
        params.put("changeName", SYSTEM_NAME);

        orderService.updateOrderState(params);
        orderProdService.updateOrderState(params);
    }
}
