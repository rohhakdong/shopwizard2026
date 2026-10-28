package com.shopwizard.payment.web;

import com.shopwizard.payment.service.PaymentGateway;
import com.shopwizard.payment.service.PaymentGatewayRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 주문서가 결제수단과 결제창 설정을 그리는 데 쓰는 공개 설정(비밀값 없음). 어떤 PG 를 쓰는지는 서버 설정
 * ({@code payment.enabled-pgs}/{@code payment.default-pg})이 정하고, 화면은 이 응답대로 그린다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentConfigController {

    private final PaymentGatewayRegistry registry;

    @GetMapping("/config")
    public Map<String, Object> config() {
        List<Map<String, Object>> pgs = new ArrayList<>();
        for (PaymentGateway g : registry.enabled()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", g.code());
            m.put("label", g.label());
            m.put("methods", g.methods());
            m.put("client", g.clientConfig());
            pgs.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("defaultPg", registry.defaultGateway().code());
        out.put("pgs", pgs);
        return out;
    }
}
