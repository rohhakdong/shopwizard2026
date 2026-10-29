package com.shopwizard.payment.service;

import com.shopwizard.payment.service.KcpWebhookVerifier.HashResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** KCP 웹훅 발신자 검증: 허용 IP, 프록시 헤더 신뢰 여부, hash_data(HMAC-SHA256) 후보 2종. */
class KcpWebhookVerifierTest {

    private static final String KEY = "test-verify-key";

    private KcpProperties props;
    private KcpWebhookVerifier verifier;

    @BeforeEach
    void setUp() {
        props = new KcpProperties();
        props.setWebhookKey(KEY);
        verifier = new KcpWebhookVerifier(props);
    }

    private static Map<String, String> payload() {
        Map<String, String> p = new HashMap<>();
        p.put("site_cd", "T0000"); p.put("tno", "26931077341432"); p.put("ordr_idxx", "SW-1");
        p.put("account", "T0123456789"); p.put("ipgm_mnyx", "26900"); p.put("noti_id", "N-1");
        p.put("ipgm_time", "20261029101500"); p.put("tx_cd", "TX00"); p.put("op_cd", "50");
        return p;
    }

    private static String plain(Map<String, String> p) {
        return String.join("^", p.get("site_cd"), p.get("tno"), p.get("ordr_idxx"), p.get("account"), p.get("ipgm_mnyx"),
                p.get("noti_id"), p.get("ipgm_time"), p.get("tx_cd"), p.get("op_cd"));
    }

    // ── IP ──────────────────────────────────────────────────────

    @Test
    void 테스트_모드는_KCP_테스트_발신_IP_만_허용한다() {
        assertThat(verifier.ipAllowed("210.122.176.144")).isTrue();
        assertThat(verifier.ipAllowed("103.215.144.173")).isFalse();   // 운영 IP 는 테스트 모드에서 불허
        assertThat(verifier.ipAllowed("1.2.3.4")).isFalse();
        assertThat(verifier.ipAllowed(null)).isFalse();
    }

    @Test
    void 운영_모드는_KCP_운영_발신_IP_세_개를_허용한다() {
        props.setMode("prod");
        assertThat(verifier.ipAllowed("103.215.144.173")).isTrue();
        assertThat(verifier.ipAllowed("103.215.144.174")).isTrue();
        assertThat(verifier.ipAllowed("210.122.72.173")).isTrue();
        assertThat(verifier.ipAllowed("210.122.176.144")).isFalse();
    }

    @Test
    void 설정한_허용_IP_가_있으면_기본값_대신_그것을_쓴다() {
        props.setWebhookAllowedIps(" 10.0.0.1 , 10.0.0.2 ");
        assertThat(verifier.ipAllowed("10.0.0.2")).isTrue();
        assertThat(verifier.ipAllowed("210.122.176.144")).isFalse();
    }

    @Test
    void 프록시_헤더는_신뢰_설정이_켜졌을_때만_쓴다() {
        assertThat(verifier.clientIp("127.0.0.1", "210.122.176.144")).isEqualTo("127.0.0.1");   // 기본: 헤더 무시(위조 방지)
        props.setWebhookTrustForwarded(true);
        assertThat(verifier.clientIp("127.0.0.1", "210.122.176.144, 10.0.0.9")).isEqualTo("210.122.176.144");
        assertThat(verifier.clientIp("127.0.0.1", null)).isEqualTo("127.0.0.1");
    }

    // ── hash_data ───────────────────────────────────────────────

    @Test
    void 필드를_이은_문자열의_HMAC_을_허용한다() {
        Map<String, String> p = payload();
        p.put("hash_data", KcpWebhookVerifier.hmacHex(KEY, plain(p)));
        assertThat(verifier.verifyHash(p)).isEqualTo(HashResult.MATCH_PLAIN);
    }

    @Test
    void 끝에_검증키를_더한_문자열의_HMAC_도_허용한다() {
        Map<String, String> p = payload();
        p.put("hash_data", KcpWebhookVerifier.hmacHex(KEY, plain(p) + "^" + KEY));
        assertThat(verifier.verifyHash(p)).isEqualTo(HashResult.MATCH_WITH_KEY);
    }

    @Test
    void 실제_모의입금_통보의_해시_필드_이름_noti_hash_도_받는다() {
        Map<String, String> p = payload();
        p.put("noti_hash", KcpWebhookVerifier.hmacHex(KEY, plain(p)));   // KCP 테스트 통보는 hash_data 가 아니라 noti_hash 로 온다
        assertThat(verifier.verifyHash(p)).isEqualTo(HashResult.MATCH_PLAIN);

        p.put("noti_hash", "0000");
        assertThat(verifier.verifyHash(p)).isEqualTo(HashResult.MISMATCH);
    }

    @Test
    void 소문자_hex_도_같은_값으로_본다() {
        Map<String, String> p = payload();
        p.put("hash_data", KcpWebhookVerifier.hmacHex(KEY, plain(p)).toLowerCase());
        assertThat(verifier.verifyHash(p)).isEqualTo(HashResult.MATCH_PLAIN);
    }

    @Test
    void 주문번호가_order_no_로만_와도_검증한다() {
        Map<String, String> p = payload();
        String hash = KcpWebhookVerifier.hmacHex(KEY, plain(p));
        p.remove("ordr_idxx");
        p.put("order_no", "SW-1");
        p.put("hash_data", hash);
        assertThat(verifier.verifyHash(p)).isEqualTo(HashResult.MATCH_PLAIN);
    }

    @Test
    void 필드가_바뀌면_거절한다() {
        Map<String, String> p = payload();
        p.put("hash_data", KcpWebhookVerifier.hmacHex(KEY, plain(p)));
        p.put("ipgm_mnyx", "1");   // 입금액을 바꿔 위조
        assertThat(verifier.verifyHash(p)).isEqualTo(HashResult.MISMATCH);
    }

    @Test
    void 다른_검증키로_만든_해시는_거절한다() {
        Map<String, String> p = payload();
        p.put("hash_data", KcpWebhookVerifier.hmacHex("other-key", plain(p)));
        assertThat(verifier.verifyHash(p)).isEqualTo(HashResult.MISMATCH);
    }

    @Test
    void hash_data_가_없으면_거절한다() {
        assertThat(verifier.verifyHash(payload())).isEqualTo(HashResult.MISMATCH);
    }

    @Test
    void 검증키가_설정되지_않으면_검사를_건너뛴다() {
        props.setWebhookKey("");
        assertThat(verifier.verifyHash(payload())).isEqualTo(HashResult.NO_KEY);
    }

    @Test
    void HMAC_결과는_대문자_hex_64자다() {
        String h = KcpWebhookVerifier.hmacHex("k", "m");
        assertThat(h).hasSize(64).matches("[0-9A-F]+");
    }
}
