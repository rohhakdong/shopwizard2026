package com.shopwizard.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KCP 웹훅 발신자 검증 — 발신 IP 와 hash_data(HMAC-SHA256).
 *
 * <p>hash 방식은 KCP 문서가 모호하다(검증키를 메시지 끝에 붙인다는 설명과 HMAC 키로 쓴다는 샘플이 함께 있다). 그래서
 * <b>키 = 검증키</b> 로 고정하고 메시지만 두 가지(필드를 '^' 로 이은 값 / 그 끝에 '^검증키' 를 더한 값)를 모두 허용한다. 어느 쪽이 맞았는지는
 * 반환값으로 알려 로그에 남기고, 라이브에서 확인된 뒤 하나로 줄인다. 어느 쪽이든 검증키를 아는 쪽만 만들 수 있으므로 보안 강도는 같다.</p>
 *
 * <p>이 검증만 믿지 않는다 — 웹훅 처리는 마지막에 KCP 거래조회로 입금 사실을 다시 확인한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KcpWebhookVerifier {

    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    /** 검증 결과. {@code NO_KEY} 는 검증키가 설정되지 않아 검사를 건너뛰었다는 뜻(테스트 모드에서만 허용). */
    public enum HashResult { MATCH_PLAIN, MATCH_WITH_KEY, MISMATCH, NO_KEY }

    private final KcpProperties props;

    /** 발신 IP 가 허용 목록에 있는지. */
    public boolean ipAllowed(String ip) {
        return ip != null && props.webhookIpSet().contains(ip.trim());
    }

    /** 요청의 발신 IP — 신뢰할 수 있는 프록시 뒤일 때만 X-Forwarded-For 의 첫 값을 쓴다. */
    public String clientIp(String remoteAddr, String forwardedFor) {
        if (props.isWebhookTrustForwarded() && forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return remoteAddr;
    }

    /** hash_data 검증. 검증키가 없으면 {@link HashResult#NO_KEY}. */
    public HashResult verifyHash(Map<String, String> p) {
        String key = props.getWebhookKey();
        if (key == null || key.isBlank()) return HashResult.NO_KEY;

        // 문서는 hash_data 라고 하지만 KCP 테스트 모의입금의 실제 통보는 noti_hash 로 보낸다 — 둘 다 받는다.
        String given = notBlank(p.get("hash_data")) ? p.get("hash_data") : p.get("noti_hash");
        if (given == null || given.isBlank()) return HashResult.MISMATCH;
        byte[] givenBytes = given.trim().toUpperCase().getBytes(EUC_KR);

        // 문서의 필드 순서. 주문번호는 ordr_idxx 로 적혀 있지만 수신 파라미터는 order_no 라 둘 다 시도한다.
        List<String> orderNos = new ArrayList<>();
        if (notBlank(p.get("ordr_idxx"))) orderNos.add(p.get("ordr_idxx"));
        if (notBlank(p.get("order_no")) && !orderNos.contains(p.get("order_no"))) orderNos.add(p.get("order_no"));
        if (orderNos.isEmpty()) orderNos.add("");

        for (String orderNo : orderNos) {
            String plain = String.join("^", v(p, "site_cd"), v(p, "tno"), orderNo, v(p, "account"), v(p, "ipgm_mnyx"),
                    v(p, "noti_id"), v(p, "ipgm_time"), v(p, "tx_cd"), v(p, "op_cd"));
            if (equalsHash(givenBytes, hmacHex(key, plain))) return HashResult.MATCH_PLAIN;
            if (equalsHash(givenBytes, hmacHex(key, plain + "^" + key))) return HashResult.MATCH_WITH_KEY;
        }
        return HashResult.MISMATCH;
    }

    /** HMAC-SHA256(키·메시지 모두 EUC-KR 바이트) → 대문자 hex. 테스트가 서명된 웹훅을 만들 때도 쓴다. */
    public static String hmacHex(String key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(EUC_KR), "HmacSHA256"));
            byte[] out = mac.doFinal(message.getBytes(EUC_KR));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02X", b));   // KCP 는 대문자 hex
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("웹훅 해시를 계산하지 못했습니다.", e);
        }
    }

    private static boolean equalsHash(byte[] given, String expectedHex) {
        return MessageDigest.isEqual(given, expectedHex.getBytes(EUC_KR));   // 상수 시간 비교
    }

    private static String v(Map<String, String> p, String k) {
        String s = p.get(k);
        return s == null ? "" : s;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
