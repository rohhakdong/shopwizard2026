package com.shopwizard.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 토스페이먼츠 결제 승인(confirm) API 호출을 담당.
 * PaymentController(/payment/confirm)와 CheckoutService(/order/checkout)에서 공용으로 사용한다.
 */
@Service
public class TossPaymentService {

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> confirm(String paymentKey, String orderId, long amount) throws Exception {
        String auth = "Basic " + Base64.getEncoder()
                .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));

        URL url = new URL(TOSS_CONFIRM_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", auth);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String body = objectMapper.writeValueAsString(Map.of(
                "paymentKey", paymentKey,
                "orderId",    orderId,
                "amount",     amount
        ));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        String respBody;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {
            respBody = br.lines().collect(Collectors.joining());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> tossResp = objectMapper.readValue(respBody, Map.class);

        if (code >= 200 && code < 300) {
            return tossResp;
        }

        String errMsg = tossResp.getOrDefault("message", "결제 승인 실패").toString();
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
    }
}
