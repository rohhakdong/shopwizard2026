package com.shopwizard.payment.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopwizard.order.model.PricedOrder;
import com.shopwizard.order.service.CheckoutPricingService;
import com.shopwizard.payment.model.KcpMobileRegisterRequest;
import com.shopwizard.payment.service.KcpGateway;
import com.shopwizard.payment.service.KcpProperties;
import com.shopwizard.payment.service.PaymentGateway;
import com.shopwizard.payment.service.PaymentGatewayRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * KCP 모바일 표준결제. PC 와 달리 결제창 스크립트가 없고 <b>거래등록 → 결제창 페이지로 폼 이동 → Ret_URL 로 인증 결과 POST</b> 순서다.
 * 인증 결과를 받은 뒤의 승인·주문 저장은 PC 와 같은 {@code POST /order/checkout} 을 쓴다.
 *
 * <ul>
 *   <li>{@code /register}: 서버가 계산한 금액으로 거래등록하고, 브라우저가 제출할 결제창 폼(action·필드)을 만들어 준다.</li>
 *   <li>{@code /return}: KCP 가 브라우저를 통해 POST 하는 Ret_URL. 본문은 EUC-KR 이다. 결과를 sessionStorage 에 담아 주문서 페이지로 돌려보낸다.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/payment/kcp")
public class KcpMobileController {

    private static final Charset EUC_KR = Charset.forName("EUC-KR");
    private static final int MAX_BODY = 64 * 1024;
    private static final int MAX_VALUE = 20_000;
    /** Ret_URL 로 돌아오는 값 중 주문서가 쓰는 것만 추린다. */
    private static final List<String> RETURN_KEYS = List.of("res_cd", "res_msg", "tran_cd", "enc_data", "enc_info", "ordr_idxx");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PaymentGatewayRegistry gatewayRegistry;
    private final CheckoutPricingService pricingService;
    private final KcpProperties props;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody KcpMobileRegisterRequest req, HttpServletRequest request) {
        PaymentGateway gateway = gatewayRegistry.require(KcpGateway.CODE);
        if (!(gateway instanceof KcpGateway kcp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KCP 결제를 사용할 수 없습니다.");
        }
        if (req.getOrderProds() == null || req.getOrderProds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문 상품 정보가 없습니다.");
        }
        // 금액은 클라이언트 값이 아니라 서버 가격 계산의 합계다. 이후 승인 때도 같은 계산으로 KCP 의 인증 금액과 대조된다.
        PricedOrder priced = pricingService.price(req.getOrderProds());
        String retUrl = baseUrl(request) + "/payment/kcp/return";

        KcpGateway.MobileRegistration reg = kcp.registerMobile(
                req.getOrderId(), priced.getTotalAmt(), req.getOrderName(), req.getPayMethod(), retUrl);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("site_cd", props.getSiteCd());
        fields.put("pay_method", reg.payMethod());
        fields.put("currency", "410");
        fields.put("Ret_URL", retUrl);
        fields.put("approval_key", reg.approvalKey());
        fields.put("PayUrl", reg.payUrl());
        fields.put("ordr_idxx", req.getOrderId());
        fields.put("good_name", truncate(req.getOrderName(), 100));
        fields.put("good_mny", String.valueOf(priced.getTotalAmt()));
        fields.put("good_cd", "");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("action", reg.formAction());
        out.put("fields", fields);
        return out;
    }

    /** Ret_URL — KCP 결제창이 브라우저를 이 주소로 POST 시킨다. 세션 쿠키는 기대하지 않는다(교차 사이트 POST). */
    @PostMapping(value = "/return", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> kcpReturn(HttpServletRequest request) throws IOException {
        Map<String, String> params = parseReturnBody(request.getInputStream().readNBytes(MAX_BODY), request);
        Map<String, String> picked = new LinkedHashMap<>();
        for (String k : RETURN_KEYS) {
            String v = params.get(k);
            if (v != null) picked.put(k, truncate(v, MAX_VALUE));
        }
        log.info("KCP 모바일 복귀 res_cd={} ordr_idxx={}", picked.get("res_cd"), picked.get("ordr_idxx"));

        String page = "<!doctype html><html lang=\"ko\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>결제 확인 중</title></head>"
                + "<body style=\"font-family:sans-serif;padding:24px\">결제 결과를 확인하고 있습니다…"
                + "<script>try{sessionStorage.setItem('kcpReturn',JSON.stringify(" + scriptSafeJson(picked) + "));}catch(e){}"
                + "location.replace(" + scriptSafeJson(request.getContextPath() + "/shop.html?kcp=return") + ");</script></body></html>";
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(page);
    }

    /**
     * 본문을 원시 바이트로 받아 EUC-KR 로 해석한다. 서블릿 컨테이너가 이미 파라미터를 읽어 본문이 비었으면(필터 등)
     * 컨테이너가 디코딩한 값으로 대신한다 — 한글 메시지만 깨질 수 있고 enc_data/enc_info 는 ASCII 라 영향이 없다.
     */
    static Map<String, String> parseReturnBody(byte[] body, HttpServletRequest request) {
        Map<String, String> out = new LinkedHashMap<>();
        if (body != null && body.length > 0) {
            for (String pair : new String(body, StandardCharsets.ISO_8859_1).split("&")) {
                if (pair.isEmpty()) continue;
                int eq = pair.indexOf('=');
                String k = eq < 0 ? pair : pair.substring(0, eq);
                String v = eq < 0 ? "" : pair.substring(eq + 1);
                try {
                    out.put(URLDecoder.decode(k, EUC_KR), URLDecoder.decode(v, EUC_KR));
                } catch (IllegalArgumentException ignore) {
                    // 잘못된 % 인코딩은 그 항목만 버린다.
                }
            }
            return out;
        }
        if (request != null) {
            request.getParameterMap().forEach((k, v) -> {
                if (v != null && v.length > 0) out.put(k, v[0]);
            });
        }
        return out;
    }

    /** &lt;script&gt; 안에 그대로 넣어도 되는 JSON — HTML 을 닫는 문자와 JS 줄 종결자를 유니코드 이스케이프한다. */
    static String scriptSafeJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value)
                    .replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026")
                    .replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String baseUrl(HttpServletRequest request) {
        String configured = props.publicBase();
        return configured.isEmpty() ? ServletUriComponentsBuilder.fromContextPath(request).build().toUriString() : configured;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
