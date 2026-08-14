package com.shopwizard.order.web;

import com.shopwizard.order.model.CheckoutRequest;
import com.shopwizard.order.service.CheckoutService;
import com.shopwizard.payment.service.TossPaymentService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
    public Map<String, Object> checkout(@RequestBody CheckoutRequest req, HttpServletRequest request) throws Exception {
        if (req.getPaymentKey() == null || req.getOrderId() == null || req.getAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 정보가 올바르지 않습니다.");
        }
        if (req.getOrderProds() == null || req.getOrderProds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문 상품 정보가 없습니다.");
        }
        if (req.getTotalAmt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액 정보가 없습니다.");
        }

        // 로그인 고객의 custId를 요청 바디의 값이 아닌 cust_id 쿠키에서 강제로 가져와,
        // 다른 고객 명의로 주문이 저장되는 것(IDOR)을 막는다.
        Integer custId = readCustIdFromCookie(request);
        if (custId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        req.setCustId(custId);

        // 1. 토스 결제 승인 (DB 트랜잭션 밖에서 먼저 수행)
        Map<String, Object> tossResp = tossPaymentService.confirm(req.getPaymentKey(), req.getOrderId(), req.getAmount());

        // 2. 주문 + 상품 + 결제 + 로그 저장 (트랜잭션). 실패 시 이미 승인된 결제를 취소해서
        //    "결제는 됐는데 주문은 없는" 상태가 남지 않도록 한다 (best-effort 보상 처리).
        try {
            int orderNo = checkoutService.checkout(req, tossResp);
            return Map.of("success", true, "orderNo", orderNo);
        } catch (Exception e) {
            try {
                tossPaymentService.cancel(req.getPaymentKey(), "주문 저장 실패로 인한 자동 취소");
            } catch (Exception cancelEx) {
                // 취소 자체가 실패해도 원래 예외를 그대로 전달 — 로그로 남겨 수동 대사(reconciliation) 대상이 되게 한다.
                e.addSuppressed(cancelEx);
            }
            throw e;
        }
    }

    private Integer readCustIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if ("cust_id".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                try {
                    return Integer.parseInt(c.getValue());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
