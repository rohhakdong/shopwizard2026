package com.shopwizard.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AlgorithmParameters;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** KCP 서명(개인키 로드·SHA256withRSA)과 REST 요청 구성. 키는 테스트가 직접 만든 임시 키라 실제 인증서와 무관하다. */
class KcpClientTest {

    private static final String CERT_PEM = "-----BEGIN CERTIFICATE-----\r\nQUJD\r\nREVG\r\n-----END CERTIFICATE-----\r\n";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir Path dir;

    private KeyPair keyPair;
    private KcpProperties props;
    private KcpCredentials credentials;
    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private volatile String responseJson = "{\"res_cd\":\"0000\"}";

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        keyPair = g.generateKeyPair();

        Files.writeString(dir.resolve("cert.pem"), CERT_PEM);
        Files.writeString(dir.resolve("key.pem"), encryptedPem(keyPair, "pw-1234"));

        props = new KcpProperties();
        props.setSiteCd("T0000");
        props.setCertPath(dir.resolve("cert.pem").toString());
        props.setPrivateKeyPath(dir.resolve("key.pem").toString());
        props.setPrivateKeyPassword("pw-1234");
        credentials = new KcpCredentials(props);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            path.set(ex.getRequestURI().getPath());
            body.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = responseJson.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        server.start();
        props.setApiBase("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    // ── 키 로드·서명 ───────────────────────────────────────────

    @Test
    void 암호화된_PKCS8_개인키를_비밀번호로_열어_SHA256withRSA_서명을_만든다() throws Exception {
        String sig = credentials.sign("T0000^1234567890^STSC");

        Signature v = Signature.getInstance("SHA256withRSA");
        v.initVerify(keyPair.getPublic());
        v.update("T0000^1234567890^STSC".getBytes(StandardCharsets.UTF_8));
        assertThat(v.verify(Base64.getDecoder().decode(sig))).isTrue();
    }

    @Test
    void 평문_PKCS8_개인키도_읽는다() throws Exception {
        String plain = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        assertThat(KcpCredentials.parsePrivateKey(plain, null).getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void 비밀번호가_틀리거나_없으면_원인만_알리고_비밀번호는_메시지에_넣지_않는다() throws Exception {
        String pem = Files.readString(dir.resolve("key.pem"));

        assertThatThrownBy(() -> KcpCredentials.parsePrivateKey(pem, "wrong-pw"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("wrong-pw").hasMessageNotContaining("pw-1234");
        assertThatThrownBy(() -> KcpCredentials.parsePrivateKey(pem, ""))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("비밀번호");
    }

    @Test
    void 인증서는_PEM_전체를_LF_줄바꿈으로_돌려준다() {
        assertThat(credentials.certInfo()).isEqualTo("-----BEGIN CERTIFICATE-----QUJDREVG-----END CERTIFICATE-----");
    }

    @Test
    void 인증서_파일이_없거나_형식이_틀리면_기동_점검에서_실패한다() throws Exception {
        props.setCertPath(dir.resolve("nope.pem").toString());
        assertThatThrownBy(() -> new KcpCredentials(props).verifyReady()).isInstanceOf(IllegalStateException.class);

        Files.writeString(dir.resolve("bad.pem"), "not a pem");
        props.setCertPath(dir.resolve("bad.pem").toString());
        assertThatThrownBy(() -> new KcpCredentials(props).verifyReady()).isInstanceOf(IllegalStateException.class);
    }

    // ── 요청 구성 ──────────────────────────────────────────────

    @Test
    void 승인_요청은_서버_값_ordr_mony_pay_type_ordr_no_와_결제창_결과를_보낸다() throws Exception {
        responseJson = "{\"res_cd\":\"0000\",\"tno\":\"T1\",\"amount\":1004}";
        KcpClient client = new KcpClient(props, credentials);

        Map<String, Object> resp = client.approve("ENC", "INFO", "00300001", 1004, "PACA", "SW-1");

        assertThat(path.get()).isEqualTo("/gw/enc/v1/payment");
        Map<String, Object> sent = sentJson();
        assertThat(sent).containsEntry("site_cd", "T0000").containsEntry("enc_data", "ENC").containsEntry("enc_info", "INFO")
                .containsEntry("tran_cd", "00300001").containsEntry("ordr_mony", "1004")
                .containsEntry("pay_type", "PACA").containsEntry("ordr_no", "SW-1");
        assertThat((String) sent.get("kcp_cert_info")).startsWith("-----BEGIN CERTIFICATE-----");
        assertThat(sent).doesNotContainKey("kcp_sign_data");   // 승인은 개인키 서명이 필요 없다
        assertThat(resp).containsEntry("tno", "T1");
    }

    @Test
    void 전체취소는_site_cd_tno_mod_type_을_서명하고_금액_필드는_보내지_않는다() throws Exception {
        KcpClient client = new KcpClient(props, credentials);

        client.cancel("12345678901234", "STSC", null, null, "단순변심");

        assertThat(path.get()).isEqualTo("/gw/mod/v1/cancel");
        Map<String, Object> sent = sentJson();
        assertThat(sent).containsEntry("tno", "12345678901234").containsEntry("mod_type", "STSC").containsEntry("mod_desc", "단순변심");
        assertThat(sent).doesNotContainKeys("mod_mny", "rem_mny");
        assertSigned((String) sent.get("kcp_sign_data"), "T0000^12345678901234^STSC");
    }

    @Test
    void 부분취소는_mod_mny_와_rem_mny_를_함께_보낸다() throws Exception {
        KcpClient client = new KcpClient(props, credentials);

        client.cancel("12345678901234", "STPC", 300L, 1000L, null);

        Map<String, Object> sent = sentJson();
        assertThat(sent).containsEntry("mod_type", "STPC").containsEntry("mod_mny", "300").containsEntry("rem_mny", "1000");
        assertSigned((String) sent.get("kcp_sign_data"), "T0000^12345678901234^STPC");
    }

    @Test
    void 거래조회는_tno_로도_주문번호로도_하고_서명은_tno_자리_값을_그대로_쓴다() throws Exception {
        KcpClient client = new KcpClient(props, credentials);

        client.inquiryByTno("12345678901234", "PACA");
        assertThat(path.get()).isEqualTo("/std/inquery");
        Map<String, Object> byTno = sentJson();
        assertThat(byTno).doesNotContainKey("mod_ordr_idxx");
        assertSigned((String) byTno.get("kcp_sign_data"), "T0000^12345678901234^PACA");

        client.inquiryByOrder("20261028", "SW-1", "PABK");
        Map<String, Object> byOrder = sentJson();
        assertThat(byOrder).containsEntry("tno", "20261028000000").containsEntry("mod_ordr_idxx", "SW-1").containsEntry("pay_type", "PABK");
        assertSigned((String) byOrder.get("kcp_sign_data"), "T0000^20261028000000^PABK");
    }

    // ── 모바일 거래등록 ─────────────────────────────────────────

    @Test
    void 모바일_거래등록은_모바일_서버로_사이트코드와_주문정보만_보낸다() throws Exception {
        props.setMobileApiBase(props.getApiBase());   // 가짜 서버로
        responseJson = "{\"Code\":\"0000\",\"Message\":\"Success\",\"approvalKey\":\"AK-1\",\"PayUrl\":\"https://x/pay/mobileGW.kcp\"}";

        Map<String, Object> resp = new KcpClient(props, credentials)
                .registerTrade("SW-1", 26900, "상품", "CARD", "https://shop.example.com/payment/kcp/return");

        assertThat(path.get()).isEqualTo("/trade/register.do");
        Map<String, Object> sent = sentJson();
        assertThat(sent).containsEntry("site_cd", "T0000").containsEntry("ordr_idxx", "SW-1")
                .containsEntry("good_mny", "26900").containsEntry("good_name", "상품")
                .containsEntry("pay_method", "CARD").containsEntry("Ret_URL", "https://shop.example.com/payment/kcp/return");
        // 거래등록에는 인증서·서명이 필요 없다 — 보내지 않는다.
        assertThat(sent).doesNotContainKeys("kcp_cert_info", "kcp_sign_data");
        assertThat(resp).containsEntry("approvalKey", "AK-1");
    }

    @Test
    void 모바일_거래등록_서버_주소는_mode_로_고른다() {
        KcpProperties p = new KcpProperties();
        assertThat(p.mobileApiBaseUrl()).isEqualTo("https://testsmpay.kcp.co.kr");
        p.setMode("prod");
        assertThat(p.mobileApiBaseUrl()).isEqualTo("https://smpay.kcp.co.kr");
        p.setMobileApiBase("http://127.0.0.1:9/");
        assertThat(p.mobileApiBaseUrl()).isEqualTo("http://127.0.0.1:9");
    }

    // ── 실패 구분 ──────────────────────────────────────────────

    @Test
    void 연결_자체가_안_되면_처리_여부가_불확실하지_않다() {
        props.setApiBase("http://127.0.0.1:1");   // 아무도 듣지 않는 포트
        props.setConnectTimeoutMs(500);
        KcpClient client = new KcpClient(props, credentials);

        assertThatThrownBy(() -> client.approve("E", "I", "T", 1000, "PACA", "SW-1"))
                .isInstanceOfSatisfying(KcpException.class, e -> assertThat(e.isUncertain()).isFalse());
    }

    @Test
    void 응답이_JSON_이_아니면_처리_여부가_불확실하다() {
        responseJson = "<html>gateway error</html>";
        KcpClient client = new KcpClient(props, credentials);

        assertThatThrownBy(() -> client.approve("E", "I", "T", 1000, "PACA", "SW-1"))
                .isInstanceOfSatisfying(KcpException.class, e -> assertThat(e.isUncertain()).isTrue());
    }

    @Test
    void 응답_코드가_실패여도_예외가_아니라_그대로_돌려준다() throws Exception {
        responseJson = "{\"res_cd\":\"S006\",\"res_msg\":\"거래금액 불일치 발생\"}";
        Map<String, Object> resp = new KcpClient(props, credentials).approve("E", "I", "T", 1000, "PACA", "SW-1");
        assertThat(resp).containsEntry("res_cd", "S006");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sentJson() throws Exception {
        return (Map<String, Object>) MAPPER.readValue(body.get(), Map.class);
    }

    private void assertSigned(String signature, String signedText) throws Exception {
        Signature v = Signature.getInstance("SHA256withRSA");
        v.initVerify(keyPair.getPublic());
        v.update(signedText.getBytes(StandardCharsets.UTF_8));
        assertThat(v.verify(Base64.getDecoder().decode(signature))).as("서명 대상: " + signedText).isTrue();
    }

    /** KCP 가 주는 것과 같은 형식(ENCRYPTED PRIVATE KEY, PBEWithSHA1AndDESede) 의 PEM 을 만든다. */
    private static String encryptedPem(KeyPair kp, String password) throws Exception {
        String alg = "PBEWithSHA1AndDESede";
        byte[] salt = new byte[8];
        new SecureRandom().nextBytes(salt);
        SecretKey sk = SecretKeyFactory.getInstance(alg).generateSecret(new PBEKeySpec(password.toCharArray()));
        Cipher c = Cipher.getInstance(alg);
        c.init(Cipher.ENCRYPT_MODE, sk, new PBEParameterSpec(salt, 2048));
        byte[] enc = c.doFinal(kp.getPrivate().getEncoded());
        AlgorithmParameters params = c.getParameters();
        byte[] der = new javax.crypto.EncryptedPrivateKeyInfo(params, enc).getEncoded();
        return "-----BEGIN ENCRYPTED PRIVATE KEY-----\r\n"
                + Base64.getMimeEncoder(64, "\r\n".getBytes()).encodeToString(der)
                + "\r\n-----END ENCRYPTED PRIVATE KEY-----\r\n";
    }
}
