package com.shopwizard.payment.web;

import com.shopwizard.order.service.TossWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 토스페이먼츠 웹훅 수신 엔드포인트.
 * 토스 개발자센터 > 웹훅 설정에서 이 URL을 등록해야 호출된다 (로컬 개발 중에는 공인 도메인이
 * 없어 ngrok 등으로 터널링해야 실제 수신 테스트가 가능하다).
 *
 * 웹훅 바디는 신뢰하지 않고 트리거로만 사용 — 실제 상태 반영은 TossWebhookService가
 * 토스 결제 조회 API를 다시 호출해서 확인한 값으로 처리한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/payment/webhook")
public class TossWebhookController {

    private final TossWebhookService tossWebhookService;

    @PostMapping("/toss")
    public Map<String, Object> handle(@RequestBody Map<String, Object> body) throws Exception {
        String eventType = String.valueOf(body.get("eventType"));

        if ("PAYMENT_STATUS_CHANGED".equals(eventType)) {
            Object dataObj = body.get("data");
            if (dataObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dataObj;
                Object paymentKey = data.get("paymentKey");
                if (paymentKey != null) {
                    tossWebhookService.handlePaymentStatusChanged(paymentKey.toString());
                }
            }
        }

        // eventType이 관심 대상이 아니어도 200을 반환해 토스가 불필요하게 재전송하지 않도록 한다.
        return Map.of("received", true);
    }
}
