package com.shopwizard.payment.service;

import com.shopwizard.payment.model.ApproveRequest;
import com.shopwizard.payment.model.CancelRequest;
import com.shopwizard.payment.model.CancelResult;
import com.shopwizard.payment.model.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** KCP 게이트웨이: 승인 검증·타임아웃 복구·취소 잔액 프로토콜. KCP 호출({@link KcpClient})은 가짜로 대신한다. */
class KcpGatewayTest {

    private KcpProperties props;
    private KcpCredentials credentials;
    private KcpClient client;
    private KcpGateway gw;

    @BeforeEach
    void setUp() {
        props = new KcpProperties();
        props.setSiteCd("T0000");
        credentials = mock(KcpCredentials.class);
        client = mock(KcpClient.class);
        gw = new KcpGateway(props, credentials, client);
    }

    private static ApproveRequest approveReq(String method, long amount) {
        ApproveRequest r = new ApproveRequest();
        r.setOrderCode("SW-1");
        r.setAmount(amount);
        r.setPayMethod(method);
        r.setPayload(Map.of("resCd", "0000", "encData", "E", "encInfo", "I", "tranCd", "00300001"));
        return r;
    }

    private static Map<String, Object> approvedCard(long amount) {
        Map<String, Object> m = new HashMap<>();
        m.put("res_cd", "0000"); m.put("res_msg", "정상처리"); m.put("tno", "T-100"); m.put("amount", amount);
        m.put("card_name", "국민카드"); m.put("app_no", "12345678"); m.put("app_time", "20261028140501");
        return m;
    }

    // ── 승인 ──────────────────────────────────────────────────

    @Test
    void 카드_승인_결과를_PG_공통_결과로_옮긴다() throws Exception {
        when(client.approve("E", "I", "00300001", 1004L, "PACA", "SW-1")).thenReturn(approvedCard(1004));

        PaymentResult r = gw.approve(approveReq("card", 1004));

        assertThat(r.getPgCode()).isEqualTo("KCP");
        assertThat(r.getTradeNo()).isEqualTo("T-100");
        assertThat(r.getOrderCode()).isEqualTo("SW-1");
        assertThat(r.getAmount()).isEqualTo(1004L);
        assertThat(r.paid()).isTrue();
        assertThat(r.getMethod()).isEqualTo("CARD");
        assertThat(r.getCardComp()).isEqualTo("국민카드");
        assertThat(r.getApproveNo()).isEqualTo("12345678");
        assertThat(r.getApprovedAt()).isEqualTo("2026-10-28 14:05:01");
    }

    @Test
    void 제휴_간편결제는_EASY_계좌이체는_TRANS_로_구분한다() throws Exception {
        Map<String, Object> easy = approvedCard(1000);
        easy.put("card_other_pay_type", "OT23");
        when(client.approve(anyString(), anyString(), anyString(), eq(1000L), eq("PACA"), anyString())).thenReturn(easy);
        assertThat(gw.approve(approveReq("card", 1000)).getMethod()).isEqualTo("EASY");

        Map<String, Object> bank = new HashMap<>();
        bank.put("res_cd", "0000"); bank.put("tno", "T-200"); bank.put("amount", 2000); bank.put("bank_name", "신한은행");
        when(client.approve(anyString(), anyString(), anyString(), eq(2000L), eq("PABK"), anyString())).thenReturn(bank);
        PaymentResult t = gw.approve(approveReq("trans", 2000));
        assertThat(t.getMethod()).isEqualTo("TRANS");
        assertThat(t.getReciptBank()).isEqualTo("신한은행");
        assertThat(t.getCardComp()).isNull();
    }

    @Test
    void 결제창_인증이_실패했거나_값이_빠졌으면_KCP_를_부르지_않고_400() throws Exception {
        ApproveRequest failed = approveReq("card", 1000);
        failed.setPayload(Map.of("resCd", "3001", "encData", "E", "encInfo", "I", "tranCd", "T"));
        ApproveRequest noEnc = approveReq("card", 1000);
        noEnc.setPayload(Map.of("resCd", "0000", "encInfo", "I", "tranCd", "T"));

        assertThatThrownBy(() -> gw.approve(failed)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> gw.approve(noEnc)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> gw.approve(approveReq("vbank", 1000))).isInstanceOf(ResponseStatusException.class);
        verify(client, never()).approve(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), any(), any());
    }

    @Test
    void KCP_가_승인을_거절하면_메시지와_함께_400이고_저장으로_넘어가지_않는다() throws Exception {
        when(client.approve(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), any(), any()))
                .thenReturn(Map.of("res_cd", "S006", "res_msg", "거래금액 불일치 발생"));

        assertThatThrownBy(() -> gw.approve(approveReq("card", 1000)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("거래금액 불일치");
    }

    @Test
    void 승인_금액이_서버_금액과_다르면_승인된_돈을_되돌리고_거절한다() throws Exception {
        when(client.approve(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), any(), any())).thenReturn(approvedCard(999));

        assertThatThrownBy(() -> gw.approve(approveReq("card", 1000))).isInstanceOf(ResponseStatusException.class);
        verify(client).cancel(eq("T-100"), eq("STSC"), isNull(), isNull(), anyString());
    }

    @Test
    void 승인_응답이_없으면_거래조회로_확인해_승인됐으면_그_결과로_이어간다() throws Exception {
        when(client.approve(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), any(), any()))
                .thenThrow(new KcpException("timeout", true, null));
        when(client.inquiryByOrder(anyString(), eq("SW-1"), eq("PACA"))).thenReturn(approvedCard(1000));

        PaymentResult r = gw.approve(approveReq("card", 1000));

        assertThat(r.getTradeNo()).isEqualTo("T-100");
        assertThat(r.paid()).isTrue();
        verify(client, never()).cancel(any(), any(), any(), any(), any());
    }

    @Test
    void 승인_응답이_없고_조회에도_없으면_504_로_실패한다() throws Exception {
        when(client.approve(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), any(), any()))
                .thenThrow(new KcpException("timeout", true, null));
        when(client.inquiryByOrder(anyString(), anyString(), anyString())).thenReturn(Map.of("res_cd", "8123", "res_msg", "거래없음"));

        assertThatThrownBy(() -> gw.approve(approveReq("card", 1000)))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(504));
    }

    @Test
    void 연결_자체가_실패하면_조회_없이_502() throws Exception {
        when(client.approve(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), any(), any()))
                .thenThrow(new KcpException("connect", false, null));

        assertThatThrownBy(() -> gw.approve(approveReq("card", 1000)))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(502));
        verify(client, never()).inquiryByOrder(any(), any(), any());
    }

    // ── 취소 ──────────────────────────────────────────────────

    private static Map<String, Object> inquiry(long rem) {
        Map<String, Object> m = new HashMap<>();
        m.put("res_cd", "0000"); m.put("amount", 1000); m.put("rem_mny", rem);
        return m;
    }

    private static CancelRequest cancelReq(Long amount, Long before) {
        CancelRequest r = new CancelRequest();
        r.setTradeNo("T-100");
        r.setReason("환불확정");
        r.setAmount(amount);
        r.setRemainingBefore(before);
        r.setPayType("100000000000");
        return r;
    }

    @Test
    void 금액이_없으면_잔액_전체를_전체취소로_부른다() throws Exception {
        when(client.inquiryByTno("T-100", "PACA")).thenReturn(inquiry(1000));
        when(client.cancel(eq("T-100"), eq("STSC"), isNull(), isNull(), anyString())).thenReturn(Map.of("res_cd", "0000"));

        CancelResult r = gw.cancel(cancelReq(null, null));

        assertThat(r.getCanceledAmount()).isEqualTo(1000L);
    }

    @Test
    void 잔액보다_적게_취소하면_부분취소로_잔액을_함께_보낸다() throws Exception {
        when(client.inquiryByTno("T-100", "PACA")).thenReturn(inquiry(1000));
        when(client.cancel("T-100", "STPC", 300L, 1000L, "환불확정")).thenReturn(Map.of("res_cd", "0000"));

        CancelResult r = gw.cancel(cancelReq(300L, 1000L));

        assertThat(r.getCanceledAmount()).isEqualTo(300L);
        verify(client).cancel("T-100", "STPC", 300L, 1000L, "환불확정");
    }

    @Test
    void 취소금액이_잔액과_같으면_부분취소가_아니라_전체취소로_부른다() throws Exception {
        when(client.inquiryByTno("T-100", "PACA")).thenReturn(inquiry(300));
        when(client.cancel(eq("T-100"), eq("STSC"), isNull(), isNull(), anyString())).thenReturn(Map.of("res_cd", "0000"));

        gw.cancel(cancelReq(300L, 300L));

        verify(client).cancel(eq("T-100"), eq("STSC"), isNull(), isNull(), anyString());
    }

    @Test
    void 재시도할_때_이미_잔액이_줄어_있으면_다시_취소하지_않는다() throws Exception {
        // 직전 잔액 1000, 이번 300 → 정상이면 700. KCP 가 이미 700 이면 앞선 호출이 성공했던 것이다.
        when(client.inquiryByTno("T-100", "PACA")).thenReturn(inquiry(700));

        CancelResult r = gw.cancel(cancelReq(300L, 1000L));

        assertThat(r.getCanceledAmount()).isEqualTo(300L);
        assertThat(r.getRaw()).containsEntry("already_canceled", true);
        verify(client, never()).cancel(any(), any(), any(), any(), any());
    }

    @Test
    void KCP_잔액이_기록과_다르면_취소하지_않고_수동_확인을_요구한다() throws Exception {
        when(client.inquiryByTno("T-100", "PACA")).thenReturn(inquiry(500));

        assertThatThrownBy(() -> gw.cancel(cancelReq(300L, 1000L)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("호출 생략");
        verify(client, never()).cancel(any(), any(), any(), any(), any());
    }

    @Test
    void 이미_전액_취소된_거래의_전체취소는_성공으로_본다() throws Exception {
        Map<String, Object> canceled = inquiry(0);
        canceled.put("stat_ca_cd", "STSC");
        when(client.inquiryByTno("T-100", "PACA")).thenReturn(canceled);

        CancelResult r = gw.cancel(cancelReq(null, null));

        assertThat(r.getRaw()).containsEntry("already_canceled", true);
        verify(client, never()).cancel(any(), any(), any(), any(), any());
    }

    @Test
    void 잔액보다_큰_금액은_취소하지_않는다() throws Exception {
        when(client.inquiryByTno("T-100", "PACA")).thenReturn(inquiry(200));

        assertThatThrownBy(() -> gw.cancel(cancelReq(300L, null))).isInstanceOf(IllegalStateException.class);
        verify(client, never()).cancel(any(), any(), any(), any(), any());
    }

    @Test
    void 취소_호출_결과를_모르면_예외로_알려_호출자가_상태를_되돌린다() throws Exception {
        when(client.inquiryByTno("T-100", "PACA")).thenReturn(inquiry(1000));
        when(client.cancel(any(), any(), any(), any(), any())).thenThrow(new KcpException("timeout", true, null));

        assertThatThrownBy(() -> gw.cancel(cancelReq(300L, 1000L))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 계좌이체_거래는_PABK_로_조회한다() throws Exception {
        CancelRequest r = cancelReq(null, null);
        r.setPayType("010000000000");
        when(client.inquiryByTno("T-100", "PABK")).thenReturn(inquiry(1000));
        when(client.cancel(any(), any(), any(), any(), any())).thenReturn(Map.of("res_cd", "0000"));

        gw.cancel(r);

        verify(client).inquiryByTno("T-100", "PABK");
    }

    // ── 설정 점검 ──────────────────────────────────────────────

    @Test
    void 운영_모드에서는_테스트_사이트코드를_쓸_수_없다() {
        props.setMode("prod");
        props.setSiteCd("T0000");
        assertThatThrownBy(() -> gw.verifyReady()).isInstanceOf(IllegalStateException.class).hasMessageContaining("T0000");

        props.setSiteCd("C9448");
        gw.verifyReady();   // 인증서 점검은 가짜 credentials 라 통과
        verify(credentials).verifyReady();
    }

    @Test
    void 모드와_사이트코드_형식이_틀리면_기동할_때_실패한다() {
        props.setMode("staging");
        assertThatThrownBy(() -> gw.verifyReady()).isInstanceOf(IllegalStateException.class);

        props.setMode("test");
        props.setSiteCd("abc");
        assertThatThrownBy(() -> gw.verifyReady()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 공개_설정에는_사이트코드와_스크립트_주소만_있고_비밀값은_없다() {
        props.setSiteName("MYSHOP");
        props.setPrivateKeyPassword("super-secret");
        props.setCertPath("C:/secret/path");

        Map<String, Object> cfg = gw.clientConfig();

        assertThat(cfg).containsEntry("siteCd", "T0000").containsEntry("siteName", "MYSHOP")
                .containsEntry("scriptUrl", "https://testspay.kcp.co.kr/plugin/kcp_spay_hub.js").containsEntry("mode", "test");
        assertThat(cfg.toString()).doesNotContain("super-secret").doesNotContain("secret/path");

        props.setMode("prod");
        assertThat(gw.clientConfig()).containsEntry("scriptUrl", "https://spay.kcp.co.kr/plugin/kcp_spay_hub.js");
    }
}
