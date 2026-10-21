package com.shopwizard.order.service;

import com.shopwizard.adjust.mapper.AdjustDetailMapper;
import com.shopwizard.order.mapper.OrderMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.mapper.ProdCodeMatchMapper;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.model.ProdMatchRequest;
import com.shopwizard.order.model.ProdMatchResult;
import com.shopwizard.order.model.ShoplinkerCollectRequest;
import com.shopwizard.order.model.ShoplinkerCollectResult;
import com.shopwizard.product.model.Prod;
import com.shopwizard.product.service.ProdService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 동시성 보호 장치: 행 잠금 순서, 재고 장부 선점(ledger-first), 이미 연결된 라인 건너뛰기, 수집 단일 실행. */
class ConcurrencyGuardsTest {

    // ───────────────────────── OrderLineLock ─────────────────────────

    @Test
    void 헤더_라인_잠금은_헤더가_먼저다_교착_방지_규칙() {
        OrderProdMapper prodMapper = mock(OrderProdMapper.class);
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderProd op = new OrderProd();
        when(prodMapper.selectLineForUpdate(anyMap())).thenReturn(op);
        OrderLineLock lock = new OrderLineLock(prodMapper, orderMapper);

        assertThat(lock.lockHeaderAndLine(7, 2)).isSameAs(op);

        InOrder order = inOrder(orderMapper, prodMapper);
        order.verify(orderMapper).selectHeaderForUpdate(7);
        order.verify(prodMapper).selectLineForUpdate(Map.of("orderNo", 7, "orderProdNo", 2));
    }

    @Test
    void 라인만_잠글_때는_헤더를_건드리지_않는다() {
        OrderProdMapper prodMapper = mock(OrderProdMapper.class);
        OrderMapper orderMapper = mock(OrderMapper.class);
        new OrderLineLock(prodMapper, orderMapper).lockLine(7, 2);

        verify(prodMapper).selectLineForUpdate(anyMap());
        verify(orderMapper, never()).selectHeaderForUpdate(anyInt());
    }

    // ───────────────────────── 상품 매칭: 장부 선점 / 이미 연결된 라인 ─────────────────────────

    private record Fixture(ProdMatchService service, OrderProdMapper orderProdMapper, OrderLineLock lineLock,
                           StockLedgerService ledger, ProdService prodService) {}

    private Fixture matchFixture(OrderProd lockedLine, boolean claimResult) {
        OrderProdMapper orderProdMapper = mock(OrderProdMapper.class);
        ProdCodeMatchMapper ruleMapper = mock(ProdCodeMatchMapper.class);
        AdjustDetailMapper adjustDetailMapper = mock(AdjustDetailMapper.class);
        ProdService prodService = mock(ProdService.class);
        StockLedgerService ledger = mock(StockLedgerService.class);
        OrderLineLock lineLock = mock(OrderLineLock.class);

        Prod prod = new Prod();
        prod.setProdCode("P1");
        when(prodService.selectList(anyMap())).thenReturn(List.of(prod));
        OrderProd listed = new OrderProd();
        listed.setOrderNo(10);
        listed.setOrderProdNo(1);
        when(orderProdMapper.selectUnmatchedLinesByName(anyMap())).thenReturn(List.of(listed));
        when(adjustDetailMapper.selectCount(anyMap())).thenReturn(0);
        when(lineLock.lockLine(10, 1)).thenReturn(lockedLine);
        when(ledger.claimDeduction(any(), any(), any(), any(), any())).thenReturn(claimResult);
        return new Fixture(new ProdMatchService(orderProdMapper, ruleMapper, adjustDetailMapper, prodService, ledger, lineLock),
                orderProdMapper, lineLock, ledger, prodService);
    }

    private static OrderProd unmatchedLine() {
        OrderProd op = new OrderProd();
        op.setOrderNo(10);
        op.setOrderProdNo(1);
        op.setProdQty(3);
        op.setSalePrice(1000);
        return op;
    }

    private static ProdMatchRequest request() {
        ProdMatchRequest req = new ProdMatchRequest();
        req.setProdName("상품");
        req.setProdOption("");
        req.setProdCode("P1");
        req.setSaveRule(false);
        req.setRegistId("admin");
        req.setRegistName("관리자");
        return req;
    }

    @Test
    void 장부를_선점했을_때만_재고를_차감한다() {
        Fixture f = matchFixture(unmatchedLine(), true);

        ProdMatchResult result = f.service().apply(request());

        assertThat(result.getApplied()).isEqualTo(1);
        InOrder order = inOrder(f.ledger(), f.prodService());
        order.verify(f.ledger()).claimDeduction(10, 1, "P1", 3, "admin");      // 장부 먼저
        order.verify(f.prodService()).updateSupplyQty(anyMap());               // 그 다음 차감
    }

    @Test
    void 이미_다른_요청이_장부를_선점했으면_재고를_또_차감하지_않는다() {
        Fixture f = matchFixture(unmatchedLine(), false);

        f.service().apply(request());

        verify(f.prodService(), never()).updateSupplyQty(anyMap());
    }

    @Test
    void 잠금_뒤_보니_이미_다른_상품에_연결된_라인은_건너뛰고_덮어쓰지_않는다() {
        OrderProd alreadyMatched = unmatchedLine();
        alreadyMatched.setProdCode("OTHER");
        Fixture f = matchFixture(alreadyMatched, true);

        ProdMatchResult result = f.service().apply(request());

        assertThat(result.getApplied()).isZero();
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getSkipDetails().get(0)).contains("이미 다른 상품에 연결됨");
        verify(f.orderProdMapper(), never()).updatePrice(any());
        verify(f.ledger(), never()).claimDeduction(any(), any(), any(), any(), any());
        verify(f.prodService(), never()).updateSupplyQty(anyMap());
    }

    // ───────────────────────── 샵링커 수집: 단일 실행 ─────────────────────────

    @Test
    void 수집이_진행_중이면_동시에_들어온_수집은_즉시_거부된다() throws Exception {
        SlnkTransService trans = mock(SlnkTransService.class);
        ShoplinkerOrderWriter writer = mock(ShoplinkerOrderWriter.class);
        ShoplinkerCollectService service = new ShoplinkerCollectService(trans, writer);

        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(trans.selectCountByResultType(anyString())).thenAnswer(inv -> {
            inside.countDown();
            release.await(10, TimeUnit.SECONDS);
            return 1;   // 첫 요청은 DB 의 "진행" 검사에서 막혀 곧 끝난다
        });

        ShoplinkerCollectRequest req = new ShoplinkerCollectRequest();
        ShoplinkerCollectRequest.MallTarget mall = new ShoplinkerCollectRequest.MallTarget();
        mall.setMallCode("");
        mall.setLoginId("");
        req.setMalls(List.of(mall));
        req.setStartDate("2026-01-01");
        req.setEndDate("2026-01-02");

        AtomicReference<ShoplinkerCollectResult> first = new AtomicReference<>();
        Thread t = new Thread(() -> first.set(service.collect(req)));
        t.start();
        assertThat(inside.await(10, TimeUnit.SECONDS)).isTrue();   // 첫 수집이 락을 쥐고 진행 중

        ShoplinkerCollectResult second = service.collect(req);

        assertThat(second.isOk()).isFalse();
        assertThat(second.getMessage()).contains("이미 진행 중");
        verify(trans, org.mockito.Mockito.times(1)).selectCountByResultType(anyString());   // 두 번째는 DB 도 보지 않고 거부

        release.countDown();
        t.join(10_000);
        assertThat(first.get().isOk()).isFalse();   // 첫 요청도 "진행 중" 행 때문에 정상 종료(락은 해제됨)
        // 락이 풀렸으니 이후 요청은 다시 DB 검사까지 간다
        when(trans.selectCountByResultType(anyString())).thenReturn(1);
        service.collect(req);
        verify(trans, org.mockito.Mockito.times(2)).selectCountByResultType(eq("진행"));
    }
}
