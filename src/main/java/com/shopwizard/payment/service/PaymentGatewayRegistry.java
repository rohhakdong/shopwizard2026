package com.shopwizard.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 사용할 수 있는 PG 목록. {@code payment.enabled-pgs}(쉼표 구분, 순서 = 화면 표시 순서)에 적힌 PG 만 새 결제에 쓰이고,
 * {@code payment.default-pg} 가 요청에 PG 가 없을 때의 기본값이다. 이미 결제된 주문의 환불·웹훅은 PG 를 꺼도 처리해야 하므로
 * {@link #forExistingPayment} 는 켜짐 여부와 상관없이 구현체를 찾는다.
 */
@Component
public class PaymentGatewayRegistry {

    private final Map<String, PaymentGateway> all = new LinkedHashMap<>();
    private final List<PaymentGateway> enabled = new ArrayList<>();
    private final PaymentGateway defaultGateway;

    public PaymentGatewayRegistry(List<PaymentGateway> gateways,
                                  @Value("${payment.enabled-pgs:TOSS}") String enabledPgs,
                                  @Value("${payment.default-pg:}") String defaultPg) {
        for (PaymentGateway g : gateways) all.put(g.code().toUpperCase(Locale.ROOT), g);

        for (String raw : enabledPgs.split(",")) {
            String code = raw.trim().toUpperCase(Locale.ROOT);
            if (code.isEmpty()) continue;
            PaymentGateway g = all.get(code);
            if (g == null) {
                throw new IllegalStateException("payment.enabled-pgs 에 알 수 없는 PG 가 있습니다: " + code + " (사용 가능: " + all.keySet() + ")");
            }
            if (!enabled.contains(g)) enabled.add(g);
        }
        if (enabled.isEmpty()) {
            throw new IllegalStateException("payment.enabled-pgs 가 비어 있습니다. 하나 이상의 PG 를 켜야 합니다.");
        }

        String def = defaultPg == null || defaultPg.isBlank() ? enabled.get(0).code() : defaultPg.trim().toUpperCase(Locale.ROOT);
        PaymentGateway d = all.get(def);
        if (d == null || !enabled.contains(d)) {
            throw new IllegalStateException("payment.default-pg(" + def + ") 가 payment.enabled-pgs 에 없습니다.");
        }
        this.defaultGateway = d;

        // 켜진 PG 만 점검한다(꺼진 PG 의 키가 없어도 앱은 떠야 한다).
        for (PaymentGateway g : enabled) g.verifyReady();
    }

    /** 새 결제에 쓸 수 있는 PG(설정 순서). */
    public List<PaymentGateway> enabled() {
        return List.copyOf(enabled);
    }

    public PaymentGateway defaultGateway() {
        return defaultGateway;
    }

    /** 새 결제용 — 코드가 비어 있으면 기본 PG, 꺼져 있거나 모르는 PG 면 400. */
    public PaymentGateway require(String code) {
        if (code == null || code.isBlank()) return defaultGateway;
        PaymentGateway g = all.get(code.trim().toUpperCase(Locale.ROOT));
        if (g == null || !enabled.contains(g)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 결제 방식입니다.");
        }
        return g;
    }

    /** 이미 결제된 주문의 환불·조회용 — PG 를 꺼 두었어도 구현체를 돌려준다. 모르는 PG 면 null. */
    public PaymentGateway forExistingPayment(String code) {
        return code == null ? null : all.get(code.trim().toUpperCase(Locale.ROOT));
    }
}
