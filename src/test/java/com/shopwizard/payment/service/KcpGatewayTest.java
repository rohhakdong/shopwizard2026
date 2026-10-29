package com.shopwizard.payment.service;

import com.shopwizard.payment.model.ApproveRequest;
import com.shopwizard.payment.model.CancelRequest;
import com.shopwizard.payment.model.CancelResult;
import com.shopwizard.payment.model.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
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

    // ── 모바일 거래등록 ─────────────────────────────────────────

    private static Map<String, Object> registered() {
        Map<String, Object> m = new HashMap<>();
        m.put("Code", "0000"); m.put("Message", "Success"); m.put("approvalKey", "AK-1");
        m.put("PayUrl", "https://testsmpay.kcp.co.kr/pay/mobileGW.kcp");
        return m;
    }

    @Test
    void 모바일_거래등록은_결제수단을_CARD_BANK_로_바꾸고_결제창_주소를_만든다() throws Exception {
        when(client.registerTrade("SW-1", 26900L, "상품", "CARD", "https://s/r")).thenReturn(registered());
        when(client.registerTrade("SW-1", 26900L, "상품", "BANK", "https://s/r")).thenReturn(registered());

        KcpGateway.MobileRegistration card = gw.registerMobile("SW-1", 26900, "상품", "card", "https://s/r");
        assertThat(card.approvalKey()).isEqualTo("AK-1");
        assertThat(card.payMethod()).isEqualTo("CARD");
        // KCP 가이드: PayUrl 의 마지막 '/' 앞부분 + /jsp/encodingFilter/encodingFilter.jsp
        assertThat(card.formAction()).isEqualTo("https://testsmpay.kcp.co.kr/pay/jsp/encodingFilter/encodingFilter.jsp");
        assertThat(gw.registerMobile("SW-1", 26900, "상품", "trans", "https://s/r").payMethod()).isEqualTo("BANK");
    }

    @Test
    void 모바일_거래등록이_거절되면_결제창을_열지_않는다() throws Exception {
        Map<String, Object> rejected = new HashMap<>();
        rejected.put("Code", "E001"); rejected.put("Message", "사이트코드 오류");
        when(client.registerTrade(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString())).thenReturn(rejected);

        assertThatThrownBy(() -> gw.registerMobile("SW-1", 1000, "상품", "card", "https://s/r"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(502));
    }

    @Test
    void 모바일_거래등록_입력이_이상하면_KCP_를_호출하지_않는다() throws Exception {
        assertThatThrownBy(() -> gw.registerMobile("SW-1", 1000, "상품", "vbank", "https://s/r")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> gw.registerMobile("bad order!", 1000, "상품", "card", "https://s/r")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> gw.registerMobile("SW-1", 0, "상품", "card", "https://s/r")).isInstanceOf(ResponseStatusException.class);
        verify(client, never()).registerTrade(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void 모바일_거래등록_연결_실패는_502() throws Exception {
        when(client.registerTrade(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString()))
                .thenThrow(new KcpException("연결 실패", false, null));
        assertThatThrownBy(() -> gw.registerMobile("SW-1", 1000, "상품", "card", "https://s/r"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(502));
    }

    // ── 가상계좌 ────────────────────────────────────────────────

    private static ApproveRequest vbankReq(long amount, String bankCode) {
        ApproveRequest r = new ApproveRequest();
        r.setOrderCode("SW-1");
        r.setAmount(amount);
        r.setPayMethod("vbank");
        r.setBuyerName("홍길동");
        r.setGoodName("상품 외 1건");
        r.setPayload(bankCode == null ? Map.of() : Map.of("bankCode", bankCode));
        return r;
    }

    private static Map<String, Object> issued(long amount) {
        Map<String, Object> m = new HashMap<>();
        m.put("res_cd", "V000"); m.put("res_msg", "정상처리"); m.put("tno", "T-900"); m.put("amount", amount);
        m.put("account", "T0123456789"); m.put("bankname", "신한은행"); m.put("bankcode", "BK26"); m.put("depositor", "샵피온");
        m.put("app_time", "20261029100000"); m.put("va_date", "20261101100000");
        return m;
    }

    @Test
    void 가상계좌를_발급하면_입금대기_결과로_계좌와_마감을_담는다() throws Exception {
        when(client.issueVirtualAccount(eq("SW-1"), eq(26900L), eq("상품 외 1건"), eq("홍길동"), eq("BK26"), eq("홍길동"), anyString()))
                .thenReturn(issued(26900));

        PaymentResult r = gw.approve(vbankReq(26900, "BK26"));

        assertThat(r.getPgCode()).isEqualTo("KCP");
        assertThat(r.getStatus()).isEqualTo(PaymentResult.STATUS_WAITING_DEPOSIT);
        assertThat(r.paid()).isFalse();                       // 발급은 결제 완료가 아니다
        assertThat(r.getMethod()).isEqualTo("VBANK");
        assertThat(r.getTradeNo()).isEqualTo("T-900");
        assertThat(r.getAmount()).isEqualTo(26900L);
        assertThat(r.getAccount()).isEqualTo("T0123456789");
        assertThat(r.getReciptBank()).isEqualTo("신한은행");
        assertThat(r.getAccountHolder()).isEqualTo("샵피온");
        assertThat(r.getDueAt()).isEqualTo("2026-11-01 10:00:00");
        assertThat(r.getExpectName()).isEqualTo("홍길동");
    }

    @Test
    void 입금_마감은_설정한_분_뒤로_14자리_형식으로_보낸다() throws Exception {
        props.setVaExpireMinutes(60);
        when(client.issueVirtualAccount(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(issued(1000));

        gw.approve(vbankReq(1000, "BK04"));

        org.mockito.ArgumentCaptor<String> vaDate = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(client).issueVirtualAccount(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString(), anyString(), vaDate.capture());
        assertThat(vaDate.getValue()).matches("\\d{14}");
        java.time.LocalDateTime due = java.time.LocalDateTime.parse(vaDate.getValue(), java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long minutes = java.time.Duration.between(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")), due).toMinutes();
        assertThat(minutes).isBetween(58L, 60L);
    }

    @Test
    void 입금자명은_14자로_자른다() throws Exception {
        when(client.issueVirtualAccount(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(issued(1000));
        ApproveRequest r = vbankReq(1000, "BK26");
        r.setBuyerName("가나다라마바사아자차카타파하거너더러머버서");   // 21자

        PaymentResult res = gw.approve(r);

        assertThat(res.getExpectName()).hasSize(14);
    }

    @Test
    void 은행코드가_없거나_목록에_없으면_KCP_를_호출하지_않는다() throws Exception {
        assertThatThrownBy(() -> gw.approve(vbankReq(1000, null))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> gw.approve(vbankReq(1000, "BK99"))).isInstanceOf(ResponseStatusException.class);
        verify(client, never()).issueVirtualAccount(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void 가상계좌_발급이_거절되면_400() throws Exception {
        Map<String, Object> rejected = new HashMap<>();
        rejected.put("res_cd", "S032"); rejected.put("res_msg", "접근권한이 없습니다.");
        when(client.issueVirtualAccount(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(rejected);

        assertThatThrownBy(() -> gw.approve(vbankReq(1000, "BK26")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void 발급_결과의_금액이_다르거나_계좌가_없으면_만들어진_계좌를_닫고_거절한다() throws Exception {
        when(client.issueVirtualAccount(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(issued(999));   // 요청은 1000

        assertThatThrownBy(() -> gw.approve(vbankReq(1000, "BK26"))).isInstanceOf(ResponseStatusException.class);

        verify(client).cancel("T-900", "STSC", null, null, "발급 결과 불일치로 자동 사용중지");
    }

    @Test
    void 발급_응답을_못_받았어도_조회로_계좌가_만들어졌으면_이어간다() throws Exception {
        when(client.issueVirtualAccount(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new KcpException("타임아웃", true, null));
        Map<String, Object> found = issued(1000);
        found.put("res_cd", "0000");   // 거래조회 응답은 0000
        when(client.inquiryByOrder(anyString(), eq("SW-1"), eq("PAVC"))).thenReturn(found);

        PaymentResult r = gw.approve(vbankReq(1000, "BK26"));

        assertThat(r.getAccount()).isEqualTo("T0123456789");
        assertThat(r.getTradeNo()).isEqualTo("T-900");
    }

    @Test
    void 발급_응답도_없고_조회에도_없으면_504() throws Exception {
        when(client.issueVirtualAccount(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new KcpException("타임아웃", true, null));
        Map<String, Object> none = new HashMap<>();
        none.put("res_cd", "SSI0");
        when(client.inquiryByOrder(anyString(), eq("SW-1"), eq("PAVC"))).thenReturn(none);

        assertThatThrownBy(() -> gw.approve(vbankReq(1000, "BK26")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(504));
    }

    @Test
    void 발급_연결_자체가_실패하면_502() throws Exception {
        when(client.issueVirtualAccount(anyString(), org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new KcpException("연결 실패", false, null));

        assertThatThrownBy(() -> gw.approve(vbankReq(1000, "BK26")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(502));
    }

    @Test
    void 입금_현황은_거래조회의_ipkum_money_를_본다() throws Exception {
        Map<String, Object> inq = new HashMap<>();
        inq.put("res_cd", "0000"); inq.put("ipkum_money", "26900"); inq.put("account", "T0123456789"); inq.put("va_date", "20261101100000");
        when(client.inquiryByTno("T-900", "PAVC")).thenReturn(inq);

        KcpGateway.DepositStatus d = gw.queryDeposit("T-900");

        assertThat(d.paidAmount()).isEqualTo(26900L);
        assertThat(d.account()).isEqualTo("T0123456789");

        inq.put("ipkum_money", "0");
        assertThat(gw.queryDeposit("T-900").paidAmount()).isZero();
    }

    @Test
    void 입금_현황_조회가_실패하면_502() throws Exception {
        Map<String, Object> inq = new HashMap<>();
        inq.put("res_cd", "S032");
        when(client.inquiryByTno("T-900", "PAVC")).thenReturn(inq);

        assertThatThrownBy(() -> gw.queryDeposit("T-900")).isInstanceOf(ResponseStatusException.class);
    }

    private static CancelRequest vbankCancel() {
        CancelRequest c = new CancelRequest();
        c.setTradeNo("T-900");
        c.setPayType("001000000000");
        c.setAmount(26900L);
        c.setReason("미입금 기한 만료");
        return c;
    }

    @Test
    void 가상계좌_취소는_사용중지를_호출한다() throws Exception {
        Map<String, Object> inq = new HashMap<>();
        inq.put("res_cd", "0000"); inq.put("ipkum_money", "0");
        when(client.inquiryByTno("T-900", "PAVC")).thenReturn(inq);
        Map<String, Object> ok = new HashMap<>();
        ok.put("res_cd", "0000");
        when(client.cancel("T-900", "STSC", null, null, "미입금 기한 만료")).thenReturn(ok);

        CancelResult r = gw.cancel(vbankCancel());

        assertThat(r.getTradeNo()).isEqualTo("T-900");
        verify(client).cancel("T-900", "STSC", null, null, "미입금 기한 만료");
        // 카드용 rem_mny 프로토콜(카드 거래조회)은 타지 않는다.
        verify(client, never()).inquiryByTno("T-900", "PACA");
    }

    @Test
    void 이미_입금된_가상계좌는_사용중지하지_않고_오류로_올린다() throws Exception {
        Map<String, Object> inq = new HashMap<>();
        inq.put("res_cd", "0000"); inq.put("ipkum_money", "26900");
        when(client.inquiryByTno("T-900", "PAVC")).thenReturn(inq);

        assertThatThrownBy(() -> gw.cancel(vbankCancel()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("이미 입금");

        verify(client, never()).cancel(anyString(), anyString(), any(), any(), any());
    }

    @Test
    void 사용중지가_거절되면_502() throws Exception {
        Map<String, Object> inq = new HashMap<>();
        inq.put("res_cd", "0000"); inq.put("ipkum_money", "0");
        when(client.inquiryByTno("T-900", "PAVC")).thenReturn(inq);
        Map<String, Object> bad = new HashMap<>();
        bad.put("res_cd", "S999"); bad.put("res_msg", "실패");
        when(client.cancel(anyString(), anyString(), any(), any(), any())).thenReturn(bad);

        assertThatThrownBy(() -> gw.cancel(vbankCancel())).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void 결제수단_목록과_설정에_가상계좌와_은행_목록이_있다() {
        assertThat(gw.methods()).extracting(m -> m.value()).contains("card", "trans", "vbank");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> banks = (List<Map<String, String>>) gw.clientConfig().get("vbankBanks");
        assertThat(banks).hasSize(13);
        assertThat(banks).extracting(b -> b.get("code")).contains("BK03", "BK04", "BK26", "BK81", "BK71");
        assertThat(banks).extracting(b -> b.get("name")).contains("신한은행", "농협");
    }

    @Test
    void 운영_모드에서는_웹훅_검증키가_없으면_기동을_막는다() {
        props.setMode("prod");
        props.setSiteCd("A1B2C");
        props.setWebhookKey("");

        assertThatThrownBy(() -> gw.verifyReady()).isInstanceOf(IllegalStateException.class).hasMessageContaining("kcp.webhook-key");

        props.setWebhookKey("verify-key");
        gw.verifyReady();   // 키가 있으면 통과(인증서 점검은 가짜)
    }

    @Test
    void 테스트_모드에서는_웹훅_검증키가_없어도_기동한다() {
        props.setMode("test");
        props.setSiteCd("T0000");
        props.setWebhookKey("");

        gw.verifyReady();
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
        props.setWebhookKey("verify-key");   // 운영 모드는 웹훅 검증키도 필수
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
