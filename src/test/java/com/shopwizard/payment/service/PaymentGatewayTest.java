package com.shopwizard.payment.service;

import com.shopwizard.payment.model.ApproveRequest;
import com.shopwizard.payment.model.CancelRequest;
import com.shopwizard.payment.model.CancelResult;
import com.shopwizard.payment.model.PayMethodOption;
import com.shopwizard.payment.model.PaymentResult;
import com.shopwizard.payment.web.PaymentConfigController;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** PG 추상화: 토스 어댑터(동작 불변), 레지스트리(켜짐/기본값/실패), 공개 설정 엔드포인트. */
class PaymentGatewayTest {

    // ── 토스 어댑터 ──────────────────────────────────────────────
    private static Map<String, Object> cardResp() {
        return Map.of("paymentKey", "pk1", "orderId", "SW-1", "status", "DONE", "method", "카드", "totalAmount", 26900,
                "approvedAt", "2026-10-23T10:00:00+09:00",
                "card", Map.of("company", "국민", "approveNo", "123456"));
    }

    @Test
    void 토스_응답을_PG_공통_결과로_바꾼다_카드() {
        PaymentResult r = TossGateway.toResult(cardResp());

        assertThat(r.getPgCode()).isEqualTo("TOSS");
        assertThat(r.getTradeNo()).isEqualTo("pk1");
        assertThat(r.getOrderCode()).isEqualTo("SW-1");
        assertThat(r.getAmount()).isEqualTo(26900L);
        assertThat(r.paid()).isTrue();
        assertThat(r.getMethod()).isEqualTo("CARD");
        assertThat(r.getCardComp()).isEqualTo("국민");
        assertThat(r.getApproveNo()).isEqualTo("123456");
        assertThat(r.getApprovedAt()).startsWith("2026-10-23 10:00");
        assertThat(r.getRaw()).containsKey("paymentKey");
    }

    @Test
    void 가상계좌_입금대기와_계좌이체_은행을_옮긴다() {
        PaymentResult vbank = TossGateway.toResult(Map.of("paymentKey", "pk2", "orderId", "SW-2", "status", "WAITING_FOR_DEPOSIT",
                "method", "가상계좌", "totalAmount", 1000, "virtualAccount", Map.of("bankCode", "088")));
        assertThat(vbank.paid()).isFalse();
        assertThat(vbank.getMethod()).isEqualTo("VBANK");
        assertThat(vbank.getReciptBank()).isEqualTo("088");

        PaymentResult trans = TossGateway.toResult(Map.of("paymentKey", "pk3", "orderId", "SW-3", "status", "DONE",
                "method", "계좌이체", "totalAmount", 1000, "transfer", Map.of("bank", "신한")));
        assertThat(trans.getMethod()).isEqualTo("TRANS");
        assertThat(trans.getReciptBank()).isEqualTo("신한");
    }

    @Test
    void 승인은_서버가_계산한_금액으로_토스_confirm_을_호출한다() throws Exception {
        TossPaymentService toss = mock(TossPaymentService.class);
        when(toss.confirm("pk1", "SW-1", 26900L)).thenReturn(cardResp());
        TossGateway gw = new TossGateway(toss);
        ApproveRequest req = new ApproveRequest();
        req.setOrderCode("SW-1");
        req.setAmount(26900L);
        req.setPayload(Map.of("paymentKey", "pk1"));

        PaymentResult r = gw.approve(req);

        assertThat(r.getTradeNo()).isEqualTo("pk1");
        verify(toss).confirm("pk1", "SW-1", 26900L);
    }

    @Test
    void 결제키나_주문번호가_없으면_토스를_호출하지_않고_400() throws Exception {
        TossPaymentService toss = mock(TossPaymentService.class);
        TossGateway gw = new TossGateway(toss);
        ApproveRequest noKey = new ApproveRequest();
        noKey.setOrderCode("SW-1");
        assertThatThrownBy(() -> gw.approve(noKey)).isInstanceOf(ResponseStatusException.class);
        verify(toss, never()).confirm(anyString(), anyString(), anyLong());
    }

    @Test
    void 전체취소와_부분취소를_토스_API_로_나눠_호출한다() throws Exception {
        TossPaymentService toss = mock(TossPaymentService.class);
        when(toss.cancel("pk1", "사유")).thenReturn(Map.of("status", "CANCELED"));
        when(toss.cancelPartial("pk1", "환불확정", 5000L, "refund-1-1")).thenReturn(Map.of("status", "PARTIAL_CANCELED"));
        TossGateway gw = new TossGateway(toss);

        CancelRequest full = new CancelRequest();
        full.setTradeNo("pk1"); full.setReason("사유");
        CancelResult f = gw.cancel(full);
        assertThat(f.getRaw()).containsEntry("status", "CANCELED");
        verify(toss).cancel("pk1", "사유");

        CancelRequest part = new CancelRequest();
        part.setTradeNo("pk1"); part.setReason("환불확정"); part.setAmount(5000L); part.setIdempotencyKey("refund-1-1");
        CancelResult p = gw.cancel(part);
        assertThat(p.getCanceledAmount()).isEqualTo(5000L);
        verify(toss).cancelPartial("pk1", "환불확정", 5000L, "refund-1-1");
    }

    // ── 레지스트리 ──────────────────────────────────────────────
    private static PaymentGateway fake(String code) {
        PaymentGateway g = mock(PaymentGateway.class);
        when(g.code()).thenReturn(code);
        return g;
    }

    @Test
    void 켜진_PG_만_새_결제에_쓰고_기본값은_설정_또는_첫번째다() {
        PaymentGateway toss = fake("TOSS");
        PaymentGateway kcp = fake("KCP");

        PaymentGatewayRegistry only = new PaymentGatewayRegistry(List.of(toss, kcp), "TOSS", "");
        assertThat(only.enabled()).containsExactly(toss);
        assertThat(only.defaultGateway()).isSameAs(toss);
        assertThat(only.require(null)).isSameAs(toss);
        assertThat(only.require("toss")).isSameAs(toss);                       // 대소문자 무관
        assertThatThrownBy(() -> only.require("KCP")).isInstanceOf(ResponseStatusException.class);   // 꺼져 있음
        assertThatThrownBy(() -> only.require("NOPE")).isInstanceOf(ResponseStatusException.class);

        PaymentGatewayRegistry both = new PaymentGatewayRegistry(List.of(toss, kcp), "KCP, TOSS", "KCP");
        assertThat(both.enabled()).containsExactly(kcp, toss);                  // 설정 순서 = 표시 순서
        assertThat(both.defaultGateway()).isSameAs(kcp);
        assertThat(both.require("")).isSameAs(kcp);
    }

    @Test
    void 이미_결제된_주문의_환불은_PG_가_꺼져_있어도_구현체를_찾는다() {
        PaymentGateway toss = fake("TOSS");
        PaymentGateway kcp = fake("KCP");
        PaymentGatewayRegistry r = new PaymentGatewayRegistry(List.of(toss, kcp), "KCP", "");

        assertThat(r.forExistingPayment("TOSS")).isSameAs(toss);
        assertThat(r.forExistingPayment("KCP")).isSameAs(kcp);
        assertThat(r.forExistingPayment("UNKNOWN")).isNull();
        assertThat(r.forExistingPayment(null)).isNull();
    }

    @Test
    void 설정이_잘못되면_기동할_때_바로_실패한다() {
        PaymentGateway toss = fake("TOSS");
        assertThatThrownBy(() -> new PaymentGatewayRegistry(List.of(toss), "KCP", "")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PaymentGatewayRegistry(List.of(toss), " , ", "")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PaymentGatewayRegistry(List.of(toss, fake("KCP")), "TOSS", "KCP")).isInstanceOf(IllegalStateException.class);
    }

    // ── 공개 설정 ──────────────────────────────────────────────
    @Test
    void 공개_설정은_켜진_PG_의_결제수단과_클라이언트_설정만_담고_비밀값은_없다() {
        TossGateway toss = new TossGateway(mock(TossPaymentService.class));
        ReflectionTestUtils.setField(toss, "clientKey", "test_ck_public");
        PaymentGatewayRegistry registry = new PaymentGatewayRegistry(List.of(toss, fake("KCP")), "TOSS", "");

        Map<String, Object> cfg = new PaymentConfigController(registry).config();

        assertThat(cfg.get("defaultPg")).isEqualTo("TOSS");
        @SuppressWarnings("unchecked") List<Map<String, Object>> pgs = (List<Map<String, Object>>) cfg.get("pgs");
        assertThat(pgs).hasSize(1);
        assertThat(pgs.get(0).get("code")).isEqualTo("TOSS");
        assertThat(pgs.get(0).get("methods")).isEqualTo(List.of(
                new PayMethodOption("card", "신용카드"), new PayMethodOption("trans", "계좌이체"), new PayMethodOption("vbank", "가상계좌")));
        assertThat(pgs.get(0).get("client")).isEqualTo(Map.of("clientKey", "test_ck_public"));
        assertThat(cfg.toString()).doesNotContain("secret");
    }
}
