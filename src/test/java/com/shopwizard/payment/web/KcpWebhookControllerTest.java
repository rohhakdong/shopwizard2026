package com.shopwizard.payment.web;

import com.shopwizard.order.service.KcpWebhookService;
import com.shopwizard.order.service.KcpWebhookService.Outcome;
import com.shopwizard.payment.service.KcpProperties;
import com.shopwizard.payment.service.KcpWebhookVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** KCP 웹훅 수신: 발신 IP·해시 검사, 업무 구분 필터, 응답 코드(result=0000 / 재전송 유도). */
class KcpWebhookControllerTest {

    private static final Charset EUC_KR = Charset.forName("EUC-KR");
    private static final String KEY = "verify-key";
    private static final String KCP_IP = "210.122.176.144";

    private KcpProperties props;
    private KcpWebhookService service;
    private KcpWebhookController controller;

    @BeforeEach
    void setUp() {
        props = new KcpProperties();
        props.setWebhookKey(KEY);
        service = mock(KcpWebhookService.class);
        controller = new KcpWebhookController(new KcpWebhookVerifier(props), service, props);
    }

    private static Map<String, String> fields(String txCd, String opCd) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("site_cd", "T0000"); p.put("tno", "26931077341432"); p.put("order_no", "SW-1"); p.put("ordr_idxx", "SW-1");
        p.put("account", "T0123456789"); p.put("ipgm_mnyx", "26900"); p.put("noti_id", "N-1");
        p.put("ipgm_time", "20261029101500"); p.put("tx_cd", txCd); p.put("op_cd", opCd); p.put("ipgm_name", "홍길동");
        return p;
    }

    private static String signed(Map<String, String> p) {
        String plain = String.join("^", p.get("site_cd"), p.get("tno"), p.get("ordr_idxx"), p.get("account"), p.get("ipgm_mnyx"),
                p.get("noti_id"), p.get("ipgm_time"), p.get("tx_cd"), p.get("op_cd"));
        return KcpWebhookVerifier.hmacHex(KEY, plain);
    }

    private static MockHttpServletRequest request(String ip, Map<String, String> p, boolean withHash) {
        Map<String, String> body = new LinkedHashMap<>(p);
        if (withHash) body.put("hash_data", signed(p));
        StringBuilder sb = new StringBuilder();
        body.forEach((k, v) -> {
            if (sb.length() > 0) sb.append('&');
            sb.append(k).append('=').append(java.net.URLEncoder.encode(v, EUC_KR));
        });
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/payment/webhook/kcp");
        r.setRemoteAddr(ip);
        r.setContent(sb.toString().getBytes(EUC_KR));
        return r;
    }

    @Test
    void 입금_통보는_처리하고_result_0000_으로_응답한다() throws Exception {
        when(service.handleDeposit("26931077341432", 26900L)).thenReturn(Outcome.PAID);

        ResponseEntity<String> res = controller.handle(request(KCP_IP, fields("TX00", "50"), true));

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).contains("value=\"0000\"");
        verify(service).handleDeposit("26931077341432", 26900L);
    }

    @Test
    void 입금_재전송_01_도_같은_방식으로_처리한다() throws Exception {
        when(service.handleDeposit(anyString(), any())).thenReturn(Outcome.ALREADY_PROCESSED);

        ResponseEntity<String> res = controller.handle(request(KCP_IP, fields("TX00", "01"), true));

        assertThat(res.getBody()).contains("value=\"0000\"");
    }

    @Test
    void 허용되지_않은_IP_는_처리하지_않고_403() throws Exception {
        ResponseEntity<String> res = controller.handle(request("6.6.6.6", fields("TX00", "50"), true));

        assertThat(res.getStatusCode().value()).isEqualTo(403);
        verify(service, never()).handleDeposit(any(), any());
    }

    @Test
    void 해시가_틀리면_처리하지_않고_403() throws Exception {
        MockHttpServletRequest forged = request(KCP_IP, fields("TX00", "50"), false);   // hash_data 없음

        ResponseEntity<String> res = controller.handle(forged);

        assertThat(res.getStatusCode().value()).isEqualTo(403);
        verify(service, never()).handleDeposit(any(), any());
    }

    @Test
    void 입금액을_위조하면_해시가_맞지_않아_거절한다() throws Exception {
        Map<String, String> p = fields("TX00", "50");
        Map<String, String> body = new LinkedHashMap<>(p);
        body.put("hash_data", signed(p));
        body.put("ipgm_mnyx", "999999");   // 서명 뒤에 금액만 바꿈
        StringBuilder sb = new StringBuilder();
        body.forEach((k, v) -> { if (sb.length() > 0) sb.append('&'); sb.append(k).append('=').append(v); });
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/payment/webhook/kcp");
        r.setRemoteAddr(KCP_IP);
        r.setContent(sb.toString().getBytes(EUC_KR));

        assertThat(controller.handle(r).getStatusCode().value()).isEqualTo(403);
        verify(service, never()).handleDeposit(any(), any());
    }

    @Test
    void 테스트_모드에서_검증키가_없으면_해시_검사를_건너뛰고_처리한다() throws Exception {
        props.setWebhookKey("");
        when(service.handleDeposit(anyString(), any())).thenReturn(Outcome.PAID);

        ResponseEntity<String> res = controller.handle(request(KCP_IP, fields("TX00", "50"), false));

        assertThat(res.getBody()).contains("value=\"0000\"");
        verify(service).handleDeposit(eq("26931077341432"), eq(26900L));
    }

    @Test
    void 운영_모드에서_검증키가_없으면_거절한다() throws Exception {
        props.setMode("prod");
        props.setWebhookKey("");

        ResponseEntity<String> res = controller.handle(request("103.215.144.173", fields("TX00", "50"), false));

        assertThat(res.getStatusCode().value()).isEqualTo(403);
        verify(service, never()).handleDeposit(any(), any());
    }

    @Test
    void 가상계좌_입금이_아닌_통보는_받았다고만_응답한다() throws Exception {
        for (String tx : new String[] {"TX01", "TX02", "TX03", "TX04"}) {
            ResponseEntity<String> res = controller.handle(request(KCP_IP, fields(tx, "50"), true));
            assertThat(res.getBody()).contains("value=\"0000\"");
        }
        verify(service, never()).handleDeposit(any(), any());
    }

    @Test
    void 입금_취소_13_은_자동으로_되돌리지_않고_받았다고만_응답한다() throws Exception {
        ResponseEntity<String> res = controller.handle(request(KCP_IP, fields("TX00", "13"), true));

        assertThat(res.getBody()).contains("value=\"0000\"");
        verify(service, never()).handleDeposit(any(), any());
    }

    @Test
    void 결제건이_아직_없으면_재전송을_받도록_0000_이_아닌_값으로_응답한다() throws Exception {
        when(service.handleDeposit(anyString(), any())).thenReturn(Outcome.RETRY);

        ResponseEntity<String> res = controller.handle(request(KCP_IP, fields("TX00", "50"), true));

        assertThat(res.getBody()).doesNotContain("value=\"0000\"").contains("name=\"result\"");
    }

    @Test
    void 처리_중_예외가_나면_재전송을_받도록_실패로_응답한다() throws Exception {
        when(service.handleDeposit(anyString(), any())).thenThrow(new IllegalStateException("KCP 조회 실패"));

        ResponseEntity<String> res = controller.handle(request(KCP_IP, fields("TX00", "50"), true));

        assertThat(res.getBody()).doesNotContain("value=\"0000\"");
    }

    @Test
    void 사람이_확인해야_하는_경우는_0000_으로_응답해_재전송을_멈춘다() throws Exception {
        when(service.handleDeposit(anyString(), any())).thenReturn(Outcome.MANUAL);

        assertThat(controller.handle(request(KCP_IP, fields("TX00", "50"), true)).getBody()).contains("value=\"0000\"");
    }

    @Test
    void 응답은_KCP_공통통보_규격의_result_hidden_input_이고_평문_result_도_함께_담는다() throws Exception {
        when(service.handleDeposit(anyString(), any())).thenReturn(Outcome.PAID);

        ResponseEntity<String> res = controller.handle(request(KCP_IP, fields("TX00", "50"), true));

        assertThat(res.getBody()).contains("<input type=\"hidden\" name=\"result\" value=\"0000\">").contains("result=0000");
    }
}
