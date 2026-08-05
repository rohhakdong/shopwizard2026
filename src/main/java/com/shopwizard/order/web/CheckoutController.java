package com.shopwizard.order.web;

import com.shopwizard.order.model.CheckoutRequest;
import com.shopwizard.order.service.CheckoutService;
import com.shopwizard.payment.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 고객 쇼핑몰(shop.html) 결제완료 콜백.
 * 토스 결제 승인 + 주문 저장을 한 번의 요청으로 처리한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/checkout")
public class CheckoutController {

    private final TossPaymentService tossPaymentService;
    private final CheckoutService checkoutService;

    @PostMapping
    public Map<String, Object> checkout(@RequestBody CheckoutRequest req) throws Exception {
        if (req.getPaymentKey() == null || req.getOrderId() == null || req.getAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 정보가 올바르지 않습니다.");
        }

        // 1. 토스 결제 승인 (DB 트랜잭션 밖에서 먼저 수행)
        Map<String, Object> tossResp = tossPaymentService.confirm(req.getPaymentKey(), req.getOrderId(), req.getAmount());

        // 2. 주문 + 상품 + 결제 + 로그 저장 (트랜잭션)
        int orderNo = checkoutService.checkout(req, tossResp);

        return Map.of("success", true, "orderNo", orderNo);
    }
}
