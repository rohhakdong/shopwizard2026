package com.shopwizard.catalog.service;

import com.shopwizard.catalog.mapper.ProdExcelMapper;
import com.shopwizard.catalog.mapper.SlinkProdMapper;
import com.shopwizard.catalog.model.Prod;
import com.shopwizard.catalog.model.SlinkSendResult;
import com.shopwizard.external.shoplinker.ShoplinkerHttp;
import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import com.shopwizard.product.mapper.SlinkLogMapper;
import com.shopwizard.product.model.SlinkLog;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 샵링커 상품 전송 — 이 테스트는 절대 실제 샵링커에 붙지 않는다(ShoplinkerHttp 를 목으로 대체). */
class SlinkProdServiceTest {

    private static final String CUSTOMER = "cust-secret-1";

    private final ProdService prodService = mock(ProdService.class);
    private final ProdExcelMapper excelMapper = mock(ProdExcelMapper.class);
    private final SlinkProdMapper slinkProdMapper = mock(SlinkProdMapper.class);
    private final SlinkLogMapper slinkLogMapper = mock(SlinkLogMapper.class);
    private final DataScopeGuard scopeGuard = mock(DataScopeGuard.class);
    private final ShoplinkerHttp http = mock(ShoplinkerHttp.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @BeforeEach
    void setUp() {
        Prod p = prod("P1");
        when(prodService.select("P1")).thenReturn(p);
        when(prodService.isPublished("P1")).thenReturn(true);
        when(prodService.selectProdDesc("P1")).thenReturn("<p>설명</p>");
        when(excelMapper.selectShopSvc("SHOP1")).thenReturn("SHP001");
        when(slinkProdMapper.selectLiveSupplyQty("P1")).thenReturn(7);
        when(request.getAttribute(AuthSession.ACTOR_ATTR)).thenReturn(new AuthSession.Actor("admin1", "관리자"));
    }

    private SlinkProdService service(String mode, String customerId) {
        return new SlinkProdService(prodService, excelMapper, slinkProdMapper, slinkLogMapper, scopeGuard, http,
                "https://slink.test/api.php", mode, customerId, "http://www.shopion.co.kr", "SHP001", 30);
    }

    private static Prod prod(String code) {
        Prod p = new Prod();
        p.setProdCode(code); p.setShopCode("SHOP1"); p.setShopProdCode("sku-" + code);
        p.setProdName("상품 " + code); p.setCateCode("G009015002000"); p.setImgUrl("/images/p/" + code + ".jpg");
        p.setSaleYn(1); p.setSalePrice(1000); p.setListPrice(1200); p.setSupplyPrice(800);
        p.setDeliFeeType("무료"); p.setVatRate(10);
        return p;
    }

    private SlinkLog lastLog() {
        ArgumentCaptor<SlinkLog> c = ArgumentCaptor.forClass(SlinkLog.class);
        verify(slinkLogMapper, atLeastOnce()).insert(c.capture());
        return c.getValue();
    }

    // ── 시험 모드(기본) ─────────────────────────────────────

    @Test
    void 시험_모드는_샵링커로_보내지_않고_이력만_남기며_계정_ID는_가린다() throws Exception {
        SlinkSendResult r = service("dry-run", CUSTOMER).send(List.of("P1"), request);

        assertEquals("dry-run", r.getMode());
        SlinkSendResult.Row row = r.getResults().get(0);
        assertEquals(SlinkSendResult.DRY, row.getStatus());
        assertTrue(row.getMessage().contains("보내지 않았습니다"));
        verifyNoInteractions(http);   // 실제 호출 0건

        SlinkLog log = lastLog();
        assertEquals("상품", log.getLogType());
        assertEquals("dry", log.getResultType());
        assertEquals("P1", log.getProdCode());
        assertEquals("admin1", log.getRegistId());
        assertTrue(log.getCallUrl().startsWith("https://slink.test/api.php?"));
        assertTrue(log.getCallUrl().contains("customer_id=***"));
        assertFalse(log.getCallUrl().contains(CUSTOMER));   // 계정 ID 는 이력에 남지 않는다
        assertTrue(log.getCallUrl().contains("partner_product_id=sku-P1"));
    }

    @Test
    void 모르는_모드_값은_안전하게_시험_모드로_본다() {
        assertEquals("dry-run", service("LIVE?", CUSTOMER).mode());
        assertEquals("dry-run", service("", CUSTOMER).mode());
        assertEquals("dry-run", service(null, CUSTOMER).mode());
        assertEquals("live", service(" Live ", CUSTOMER).mode());
    }

    @Test
    void 시험_모드는_계정_ID가_없어도_된다() {
        SlinkSendResult r = service("dry-run", "").send(List.of("P1"), request);
        assertEquals(SlinkSendResult.DRY, r.getResults().get(0).getStatus());
    }

    // ── 실전송 ──────────────────────────────────────────────

    @Test
    void 실전송은_계정_ID가_없으면_아무것도_하지_않고_거부() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service("live", "").send(List.of("P1"), request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertTrue(e.getReason().contains("SHOPLINKER_CUSTOMER_ID"));
        verifyNoInteractions(http, slinkLogMapper);
    }

    @Test
    void 실전송_성공은_샵링커_상품ID를_보여주고_실제_계정ID로_호출하되_이력에는_가린다() throws Exception {
        when(http.get(anyString(), anyInt())).thenReturn("true|prd149987748|성공");

        SlinkSendResult r = service("live", CUSTOMER).send(List.of("P1"), request);

        SlinkSendResult.Row row = r.getResults().get(0);
        assertEquals(SlinkSendResult.SENT, row.getStatus());
        assertTrue(row.getMessage().contains("prd149987748"));
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(http).get(url.capture(), eq(30));
        assertTrue(url.getValue().contains("customer_id=" + CUSTOMER));   // 실제 호출에는 실제 ID
        assertTrue(url.getValue().contains("quantity=7"));                  // 판매 상품의 현재 재고
        SlinkLog log = lastLog();
        assertEquals("true", log.getResultType());
        assertFalse(log.getCallUrl().contains(CUSTOMER));
    }

    @Test
    void 샵링커가_거절하면_실패로_이력에_남긴다() throws Exception {
        when(http.get(anyString(), anyInt())).thenReturn("false - 상품이미지 누락");

        SlinkSendResult.Row row = service("live", CUSTOMER).send(List.of("P1"), request).getResults().get(0);

        assertEquals(SlinkSendResult.FAILED, row.getStatus());
        assertTrue(row.getMessage().contains("상품이미지 누락"));
        assertEquals("false", lastLog().getResultType());
    }

    @Test
    void 호출_예외는_실패로_이력에_남고_다음_상품은_계속한다() throws Exception {
        when(prodService.select("P2")).thenReturn(prod("P2"));
        when(prodService.isPublished("P2")).thenReturn(true);
        when(http.get(contains("sku-P1"), anyInt())).thenThrow(new java.net.http.HttpTimeoutException("timed out"));
        when(http.get(contains("sku-P2"), anyInt())).thenReturn("true|prd2|성공");

        SlinkSendResult r = service("live", CUSTOMER).send(List.of("P1", "P2"), request);

        assertEquals(SlinkSendResult.FAILED, r.getResults().get(0).getStatus());
        assertTrue(r.getResults().get(0).getMessage().contains("전송 오류"));
        assertEquals(SlinkSendResult.SENT, r.getResults().get(1).getStatus());
        ArgumentCaptor<SlinkLog> logs = ArgumentCaptor.forClass(SlinkLog.class);
        verify(slinkLogMapper, times(2)).insert(logs.capture());
        assertEquals("error", logs.getAllValues().get(0).getResultType());
        assertEquals("true", logs.getAllValues().get(1).getResultType());
    }

    @Test
    void 이력_기록이_실패해도_전송_결과는_그대로_돌려준다() throws Exception {
        when(http.get(anyString(), anyInt())).thenReturn("true|prd1|성공");
        doThrow(new RuntimeException("db down")).when(slinkLogMapper).insert(any());

        SlinkSendResult.Row row = service("live", CUSTOMER).send(List.of("P1"), request).getResults().get(0);

        assertEquals(SlinkSendResult.SENT, row.getStatus());
    }

    // ── 재고 규칙 ───────────────────────────────────────────

    @Test
    void 재고가_없거나_마이너스_1000이면_무제한_표시_10000() throws Exception {
        when(slinkProdMapper.selectLiveSupplyQty("P1")).thenReturn(null);
        service("dry-run", CUSTOMER).send(List.of("P1"), request);
        assertTrue(lastLog().getCallUrl().contains("quantity=10000"));

        when(slinkProdMapper.selectLiveSupplyQty("P1")).thenReturn(-1000);
        service("dry-run", CUSTOMER).send(List.of("P1"), request);
        assertTrue(lastLog().getCallUrl().contains("quantity=10000"));

        when(slinkProdMapper.selectLiveSupplyQty("P1")).thenReturn(0);
        service("dry-run", CUSTOMER).send(List.of("P1"), request);
        assertTrue(lastLog().getCallUrl().contains("quantity=0"));   // 품절은 0 그대로
    }

    // ── 건너뜀 (호출도 이력도 없음) ─────────────────────────

    private void assertSkipped(SlinkSendResult.Row row, String reasonPart) {
        assertEquals(SlinkSendResult.SKIPPED, row.getStatus());
        assertTrue(row.getMessage().contains(reasonPart), row.getMessage());
        verifyNoInteractions(http);
        verify(slinkLogMapper, never()).insert(any());
    }

    @Test
    void 승인_게시되지_않은_상품은_건너뛴다() {
        when(prodService.isPublished("P1")).thenReturn(false);
        assertSkipped(service("live", CUSTOMER).send(List.of("P1"), request).getResults().get(0), "승인");
    }

    @Test
    void 샵피온이_아닌_서비스_상품은_건너뛴다() {
        when(excelMapper.selectShopSvc("SHOP1")).thenReturn("CSH001");
        assertSkipped(service("live", CUSTOMER).send(List.of("P1"), request).getResults().get(0), "SHP001");
    }

    @Test
    void 필수값이_없으면_사유와_함께_건너뛴다() {
        Prod p = prod("P1");
        p.setImgUrl(null);
        p.setShopProdCode(null);
        when(prodService.select("P1")).thenReturn(p);

        SlinkSendResult.Row row = service("live", CUSTOMER).send(List.of("P1"), request).getResults().get(0);

        assertSkipped(row, "이미지");
        assertTrue(row.getMessage().contains("상점상품코드"));
    }

    @Test
    void 없는_상품과_권한_없는_상품은_건너뛴다() {
        when(prodService.select("NOPE")).thenReturn(null);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(scopeGuard).requireCatProd(any(), eq("P9"));

        SlinkSendResult r = service("live", CUSTOMER).send(List.of("NOPE", "P9"), request);

        assertTrue(r.getResults().get(0).getMessage().contains("존재하지 않는"));
        assertTrue(r.getResults().get(1).getMessage().contains("권한이 없는"));
        verify(prodService, never()).select("P9");   // 권한 검사가 먼저 — 남의 상품 정보는 읽지 않는다
        verifyNoInteractions(http);
    }

    @Test
    void 요청이_8000자를_넘으면_보내지_않는다() {
        when(prodService.selectProdDesc("P1")).thenReturn("가".repeat(3000));   // EUC-KR 인코딩 후 9,000자

        SlinkSendResult.Row row = service("live", CUSTOMER).send(List.of("P1"), request).getResults().get(0);

        assertSkipped(row, "너무 깁니다");
    }

    // ── 입력 검사 ───────────────────────────────────────────

    @Test
    void 선택이_없거나_50건을_넘으면_거부() {
        SlinkProdService s = service("dry-run", CUSTOMER);
        assertThrows(ResponseStatusException.class, () -> s.send(null, request));
        assertThrows(ResponseStatusException.class, () -> s.send(List.of(), request));
        assertThrows(ResponseStatusException.class, () -> s.send(List.of(" ", ""), request));

        List<String> many = new ArrayList<>();
        for (int i = 0; i < SlinkProdService.MAX_PRODUCTS + 1; i++) many.add("P" + i);
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> s.send(many, request));
        assertTrue(e.getReason().contains("50"));
        verifyNoInteractions(http, slinkLogMapper);
    }

    @Test
    void 같은_상품코드는_한_번만_처리한다() {
        SlinkSendResult r = service("dry-run", CUSTOMER).send(List.of("P1", "P1", " P1 "), request);

        assertEquals(1, r.getResults().size());
        verify(slinkLogMapper, times(1)).insert(any());
    }

    @Test
    void 이력_조회는_개수를_1에서_100으로_제한한다() {
        service("dry-run", CUSTOMER).logs("P1", 0);
        service("dry-run", CUSTOMER).logs("P1", 100000);

        verify(slinkProdMapper).selectProdLogs("P1", 1);
        verify(slinkProdMapper).selectProdLogs("P1", 100);
    }
}
