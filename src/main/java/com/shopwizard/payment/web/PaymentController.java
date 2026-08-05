package com.shopwizard.payment.web;

import com.shopwizard.payment.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {

    private final TossPaymentService tossPaymentService;

    @PostMapping("/confirm")
    public Map<String, Object> confirm(@RequestBody Map<String, Object> req) throws Exception {
        String paymentKey = (String) req.get("paymentKey");
        String orderId    = (String) req.get("orderId");
        long   amount     = ((Number) req.get("amount")).longValue();

        Map<String, Object> tossResp = tossPaymentService.confirm(paymentKey, orderId, amount);
        return Map.of("success", true, "toss", tossResp);
    }
}
