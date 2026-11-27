package com.shopwizard.ship.service;

import com.shopwizard.external.shoplinker.ShoplinkerHttp;
import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.product.mapper.SlinkLogMapper;
import com.shopwizard.product.model.SlinkLog;
import com.shopwizard.ship.mapper.SlnkInvoiceMapper;
import com.shopwizard.ship.model.SlnkInvoiceLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 샵링커 송장 전송 — 대상 판정(skip), 시험 모드는 보내지 않음, live 성공/실패/예외/계정 없음, 이력 기록, 택배사명 매핑, 재전송 중복 제거. */
class SlnkInvoiceServiceTest {

    private static final String URL = "https://slink.test/delivery.php";
    private static final String EXCLUDED = "EX1,EX2";
    private ShoplinkerHttp http;
    private SlinkLogMapper logMapper;
    private SlnkInvoiceMapper mapper;
    private static final AuthSession.Actor ACTOR = new AuthSession.Actor("u", "사용자");

    @BeforeEach
    void setUp() {
        http = mock(ShoplinkerHttp.class);
        logMapper = mock(SlinkLogMapper.class);
        mapper = mock(SlnkInvoiceMapper.class);
    }

    private SlnkInvoiceService service(String mode, String customerId) {
        return new SlnkInvoiceService(http, logMapper, mapper, URL, mode, customerId, 10, EXCLUDED);
    }

    private static SlnkInvoiceLine line() {
        SlnkInvoiceLine l = new SlnkInvoiceLine();
        l.setOrderNo(100);
        l.setOrderProdNo(1);
        l.setOrderChangeNo(1);
        l.setChnlCode("CH1");
        l.setSlnkOrderCode("SLNK-9");
        l.setDeliCompName("CJ-GLS택배");
        l.setInvNo("123456789012");
        return l;
    }

    private SlinkLog loggedOnce() {
        ArgumentCaptor<SlinkLog> c = ArgumentCaptor.forClass(SlinkLog.class);
        verify(logMapper).insert(c.capture());
        return c.getValue();
    }

    @Test
    void 샵링커_주문코드가_없거나_제외_채널이거나_송장이_없으면_보내지도_기록하지도_않는다() {
        SlnkInvoiceService s = service("live", "acct");
        SlnkInvoiceLine none = line();
        none.setSlnkOrderCode(" ");
        assertThat(s.push(none, ACTOR)).extracting(SlnkInvoiceService.Result::status).isEqualTo(SlnkInvoiceService.SKIP);
        SlnkInvoiceLine ex = line();
        ex.setChnlCode("EX2");
        assertThat(s.push(ex, ACTOR).message()).contains("보내지 않는 채널");
        SlnkInvoiceLine noInv = line();
        noInv.setInvNo("");
        assertThat(s.push(noInv, ACTOR).status()).isEqualTo(SlnkInvoiceService.SKIP);

        verifyNoInteractions(http);
        verifyNoInteractions(logMapper);
    }

    @Test
    void 모르는_모드는_시험_모드이고_시험_모드는_샵링커를_부르지_않고_dry_이력만_남긴다() {
        SlnkInvoiceService s = service("production??", "acct");

        SlnkInvoiceService.Result r = s.push(line(), ACTOR);

        assertThat(s.mode()).isEqualTo("dry-run");
        assertThat(r.status()).isEqualTo(SlnkInvoiceService.DRY);
        verifyNoInteractions(http);
        SlinkLog l = loggedOnce();
        assertThat(l.getLogType()).isEqualTo("송장");
        assertThat(l.getResultType()).isEqualTo("dry");
        assertThat(l.getOrderNo()).isEqualTo(100);
        assertThat(l.getInvNo()).isEqualTo("123456789012");
        assertThat(l.getCallUrl()).contains("customer_id=***").doesNotContain("acct");
        assertThat(l.getRegistId()).isEqualTo("u");
    }

    @Test
    void live_성공은_true_이력_실패는_false_이력이고_저장_결과를_바꾸지_않는다() throws Exception {
        when(http.get(anyString(), eq(10))).thenReturn("true", "false - 주문 없음");
        SlnkInvoiceService s = service("live", "acct");

        SlnkInvoiceService.Result ok = s.push(line(), ACTOR);
        SlnkInvoiceService.Result bad = s.push(line(), ACTOR);

        assertThat(ok.status()).isEqualTo(SlnkInvoiceService.SENT);
        assertThat(bad.status()).isEqualTo(SlnkInvoiceService.FAILED);
        assertThat(bad.message()).contains("주문 없음");
        ArgumentCaptor<SlinkLog> c = ArgumentCaptor.forClass(SlinkLog.class);
        verify(logMapper, times(2)).insert(c.capture());
        assertThat(c.getAllValues()).extracting(SlinkLog::getResultType).containsExactly("true", "false");
        verify(http, times(2)).get(contains("customer_id=acct&order_id=SLNK-9"), eq(10));
    }

    @Test
    void live_통신_예외는_error_이력으로_남기고_예외를_던지지_않는다() throws Exception {
        when(http.get(anyString(), anyInt())).thenThrow(new java.io.IOException("timeout"));

        SlnkInvoiceService.Result r = service("live", "acct").push(line(), ACTOR);

        assertThat(r.status()).isEqualTo(SlnkInvoiceService.FAILED);
        assertThat(r.message()).contains("IOException").contains("timeout");
        assertThat(loggedOnce().getResultType()).isEqualTo("error");
    }

    @Test
    void live_인데_계정_ID가_없으면_호출하지_않고_실패로_기록한다() {
        SlnkInvoiceService.Result r = service("live", " ").push(line(), ACTOR);

        assertThat(r.status()).isEqualTo(SlnkInvoiceService.FAILED);
        assertThat(r.message()).contains("SHOPLINKER_CUSTOMER_ID");
        verifyNoInteractions(http);
        assertThat(loggedOnce().getResultType()).isEqualTo("error");
    }

    @Test
    void 이력_기록이_실패해도_전송_결과는_그대로_돌려준다() throws Exception {
        when(http.get(anyString(), anyInt())).thenReturn("true");
        doThrow(new IllegalStateException("log db down")).when(logMapper).insert(any());

        assertThat(service("live", "acct").push(line(), ACTOR).status()).isEqualTo(SlnkInvoiceService.SENT);
    }

    @Test
    void 택배사_마스터의_샵링커_택배사명이_있으면_그_이름을_보낸다() throws Exception {
        when(http.get(anyString(), anyInt())).thenReturn("true");
        SlnkInvoiceLine l = line();
        l.setSlnkDeliName("CJ대한통운");

        service("live", "acct").push(l, ACTOR);

        // CJ대한통운 의 EUC-KR 인코딩이 들어가고 우리 택배사명(CJ-GLS)은 안 나간다
        verify(http).get(contains("delivery_name=CJ%B4%EB%C7%D1%C5%EB%BF%EE"), anyInt());
        assertThat(loggedOnce().getDeliCompName()).isEqualTo("CJ대한통운");
        assertThat(SlnkInvoiceService.deliveryName(line())).isEqualTo("CJ-GLS택배");
    }

    @Test
    void 줄을_읽어_보내고_없는_줄은_skip() {
        when(mapper.selectLine(100, 1, 1)).thenReturn(line());
        SlnkInvoiceService s = service("dry-run", "");

        assertThat(s.pushLine(100, 1, 1, ACTOR).status()).isEqualTo(SlnkInvoiceService.DRY);
        assertThat(s.pushLine(9, 9, 9, ACTOR).status()).isEqualTo(SlnkInvoiceService.SKIP);
    }

    @Test
    void 재전송은_같은_주문코드_택배사_송장_조합을_한_번만_호출한다() throws Exception {
        SlnkInvoiceLine a = line();
        SlnkInvoiceLine b = line();
        b.setOrderChangeNo(2);   // 같은 샵링커 주문코드·송장
        SlnkInvoiceLine c = line();
        c.setOrderNo(101);
        c.setSlnkOrderCode("SLNK-10");
        when(mapper.selectLine(100, 1, 1)).thenReturn(a);
        when(mapper.selectLine(100, 1, 2)).thenReturn(b);
        when(mapper.selectLine(101, 1, 1)).thenReturn(c);
        when(http.get(anyString(), anyInt())).thenReturn("true");

        Map<String, SlnkInvoiceService.Result> r = service("live", "acct").resend(List.of(new int[]{100, 1, 1}, new int[]{100, 1, 2}, new int[]{101, 1, 1}, new int[]{5, 5, 5}), ACTOR);

        verify(http, times(2)).get(anyString(), anyInt());
        assertThat(r).containsKeys("100-1-1", "100-1-2", "101-1-1", "5-5-5");
        assertThat(r.get("100-1-2").status()).isEqualTo(SlnkInvoiceService.SENT);
        assertThat(r.get("5-5-5").status()).isEqualTo(SlnkInvoiceService.SKIP);
        verify(logMapper, times(2)).insert(any());
    }

    @Test
    void 미전송_조회는_기간_제외채널_범위를_넘기고_모드를_알려_준다() throws Exception {
        when(mapper.countPending(anyInt(), any(), any(), any(), any())).thenReturn(3);
        when(mapper.selectPending(anyInt(), any(), any(), any(), any(), anyInt())).thenReturn(List.of(line()));

        Map<String, Object> p = service("dry-run", "").pending("S1", " ", "SHP001");

        assertThat(p).containsEntry("mode", "dry-run").containsEntry("total", 3).containsEntry("days", SlnkInvoiceService.PENDING_DAYS);
        verify(mapper).countPending(eq(SlnkInvoiceService.PENDING_DAYS), any(), eq("S1"), isNull(), eq("SHP001"));
        ArgumentCaptor<List<String>> ex = ArgumentCaptor.forClass(List.class);
        verify(mapper).selectPending(anyInt(), ex.capture(), any(), any(), any(), eq(SlnkInvoiceService.PENDING_LIMIT));
        assertThat(ex.getValue()).containsExactlyInAnyOrder("EX1", "EX2");
        verify(http, never()).get(anyString(), anyInt());
    }
}
