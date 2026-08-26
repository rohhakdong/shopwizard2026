package com.shopwizard.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 토스페이먼츠 결제 승인(confirm)/취소(cancel) API 호출을 담당.
 * PaymentController(/payment/confirm)와 CheckoutService(/order/checkout)에서 공용으로 사용한다.
 */
@Service
public class TossPaymentService {

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_CANCEL_URL  = "https://api.tosspayments.com/v1/payments/%s/cancel";
    private static final String TOSS_PAYMENT_URL = "https://api.tosspayments.com/v1/payments/%s";

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> confirm(String paymentKey, String orderId, long amount) throws Exception {
        return call(TOSS_CONFIRM_URL, "POST", Map.of(
                "paymentKey", paymentKey,
                "orderId",    orderId,
                "amount",     amount
        ));
    }

    /** 주문 저장 실패 등으로 승인된 결제를 되돌려야 할 때 사용하는 결제취소 API 호출 (best-effort). */
    public Map<String, Object> cancel(String paymentKey, String cancelReason) throws Exception {
        return call(String.format(TOSS_CANCEL_URL, paymentKey), "POST", Map.of("cancelReason", cancelReason));
    }

    /**
     * paymentKey로 토스 서버에 결제 상태를 직접 조회한다 (결제 조회 API).
     * 웹훅은 트리거로만 사용하고, 실제 상태 반영은 이 API의 응답을 신뢰해서 처리한다
     * (웹훅 바디 자체는 위변조될 수 있으므로 그대로 믿지 않는다).
     */
    public Map<String, Object> getPayment(String paymentKey) throws Exception {
        return call(String.format(TOSS_PAYMENT_URL, paymentKey), "GET", null);
    }

    private Map<String, Object> call(String urlStr, String method, Map<String, Object> body) throws Exception {
        String auth = "Basic " + Base64.getEncoder()
                .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        try {
            conn.setRequestMethod(method);
            conn.setRequestProperty("Authorization", auth);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);

            if (body != null) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
                }
            }

            int code = conn.getResponseCode();
            String respBody = readBody(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = objectMapper.readValue(respBody, Map.class);

            if (code >= 200 && code < 300) {
                return resp;
            }

            String errMsg = resp.getOrDefault("message", "토스페이먼츠 API 호출 실패").toString();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        } finally {
            conn.disconnect();
        }
    }

    private static String readBody(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining());
        }
    }
}
