package com.shopwizard.adjust.service;

import com.shopwizard.adjust.mapper.AdjustDetailMapper;
import com.shopwizard.adjust.mapper.AdjustMapper;
import com.shopwizard.adjust.mapper.AdjustProcessMapper;
import com.shopwizard.adjust.model.Adjust;
import com.shopwizard.adjust.model.AdjustDetail;
import com.shopwizard.adjust.model.AdjustNoSeq;
import com.shopwizard.adjust.model.AdjustProcessRequests.BatchResult;
import com.shopwizard.adjust.model.AdjustProcessRequests.CloseRequest;
import com.shopwizard.adjust.model.AdjustProcessRequests.CreateRequest;
import com.shopwizard.adjust.model.AdjustProcessRequests.PriceRequest;
import com.shopwizard.adjust.web.AdjustProcessController;
import com.shopwizard.framework.auth.AuthSession;
import com.shopwizard.framework.auth.DataScopeGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 정산 생성·취소·금액 수정·상점 마감 — 한 건이 한 번만 들어가고, 마감 후에는 건드릴 수 없다. */
class AdjustProcessTest {

    private AdjustProcessMapper mapper;
    private AdjustMapper adjustMapper;
    private AdjustDetailMapper detailMapper;
    private AdjustDetailWriter writer;
    private AdjustProcessService service;

    @BeforeEach
    void setUp() {
        mapper = mock(AdjustProcessMapper.class);
        adjustMapper = mock(AdjustMapper.class);
        detailMapper = mock(AdjustDetailMapper.class);
        writer = new AdjustDetailWriter(mapper, adjustMapper, detailMapper);
        service = new AdjustProcessService(mapper, writer);
    }

    // ───────────────────────── 정산자료 생성 ─────────────────────────

    @Test
    void 배송_건은_선택일을_조건부로_찍고_한_건만_복사한다() {
        when(mapper.markShipSelected(anyMap())).thenReturn(1);
        when(mapper.insertDetailFromShip(anyMap())).thenReturn(1);

        assertThat(writer.createOne("SHIP", 7, 2, 1, "admin", "관리자")).isNull();

        InOrder order = inOrder(mapper);
        order.verify(mapper).markShipSelected(anyMap());
        order.verify(mapper).insertDetailFromShip(anyMap());
        verify(mapper, never()).markReturnSelected(anyMap());
        verify(mapper, never()).insertDetailFromReturn(anyMap());
    }

    @Test
    void 반품_건은_반품_쪽_쿼리만_쓴다() {
        when(mapper.markReturnSelected(anyMap())).thenReturn(1);
        when(mapper.insertDetailFromReturn(anyMap())).thenReturn(1);

        assertThat(writer.createOne("RETURN", 7, 2, 1, "admin", "관리자")).isNull();

        verify(mapper, never()).markShipSelected(anyMap());
        verify(mapper, never()).insertDetailFromShip(anyMap());
    }

    @Test
    void 이미_선택된_건은_복사하지_않고_사유를_돌려준다_이중_정산_방지() {
        when(mapper.markShipSelected(anyMap())).thenReturn(0);

        assertThat(writer.createOne("SHIP", 7, 2, 1, "admin", "관리자")).contains("이미 정산자료로 선택됐거나");

        verify(mapper, never()).insertDetailFromShip(anyMap());
    }

    @Test
    void 복사가_1건이_아니면_예외로_롤백한다_선택일만_찍힌_상태를_남기지_않는다() {
        when(mapper.markShipSelected(anyMap())).thenReturn(1);
        when(mapper.insertDetailFromShip(anyMap())).thenReturn(0);

        assertThatThrownBy(() -> writer.createOne("SHIP", 7, 2, 1, "admin", "관리자"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 구분이나_키가_잘못되면_아무것도_쓰지_않는다() {
        assertThat(writer.createOne("BOGUS", 7, 2, 1, "a", "b")).contains("구분");
        assertThat(writer.createOne("SHIP", null, 2, 1, "a", "b")).contains("키");
        verifyNoInteractions(mapper);
    }

    @Test
    void 생성_배치는_건별_성공_실패_예외를_집계한다() {
        when(mapper.markShipSelected(anyMap())).thenReturn(1, 0, 1);
        when(mapper.insertDetailFromShip(anyMap())).thenReturn(1, 0);   // 세 번째 건은 복사 실패 → 예외

        CreateRequest req = new CreateRequest();
        req.setItems(List.of(item("SHIP", 1), item("SHIP", 2), item("SHIP", 3)));

        BatchResult r = service.create(req, "admin", "관리자");

        assertThat(r.getSuccess()).isEqualTo(1);
        assertThat(r.getFailed()).isEqualTo(2);
        assertThat(r.getFailDetails().get(0)).startsWith("2-1(SHIP) :");
        assertThat(r.getFailDetails().get(1)).startsWith("3-1(SHIP) :");
    }

    @Test
    void 생성_배치는_한_번에_500건까지() {
        CreateRequest req = new CreateRequest();
        req.setItems(java.util.Collections.nCopies(501, item("SHIP", 1)));
        assertThatThrownBy(() -> service.create(req, "a", "b")).isInstanceOf(ResponseStatusException.class);
    }

    private static CreateRequest.Item item(String kind, int orderNo) {
        CreateRequest.Item it = new CreateRequest.Item();
        it.setKind(kind);
        it.setOrderNo(orderNo);
        it.setOrderProdNo(1);
        it.setOrderChangeNo(1);
        return it;
    }

    // ───────────────────────── 생성 취소 ─────────────────────────

    private static AdjustDetail detail(int qty) {
        AdjustDetail d = new AdjustDetail();
        d.setAdjustDetailNo(9);
        d.setOrderNo(7);
        d.setOrderProdNo(2);
        d.setOrderChangeNo(1);
        d.setProdQty(qty);
        return d;
    }

    @Test
    void 마감_전_배송_자료는_삭제하고_배송_행의_선택일을_푼다() {
        when(mapper.selectDetailForUpdate(9)).thenReturn(detail(3));
        when(mapper.unmarkShipSelected(anyMap())).thenReturn(1);
        when(mapper.deleteOpenDetail(9)).thenReturn(1);

        assertThat(writer.cancelOne(9, "admin", "관리자")).isNull();

        verify(mapper).unmarkShipSelected(anyMap());
        verify(mapper, never()).unmarkReturnSelected(anyMap());
    }

    @Test
    void 수량이_음수인_자료는_반품_행의_선택일을_푼다() {
        when(mapper.selectDetailForUpdate(9)).thenReturn(detail(-3));
        when(mapper.unmarkReturnSelected(anyMap())).thenReturn(1);
        when(mapper.deleteOpenDetail(9)).thenReturn(1);

        assertThat(writer.cancelOne(9, "admin", "관리자")).isNull();

        verify(mapper).unmarkReturnSelected(anyMap());
        verify(mapper, never()).unmarkShipSelected(anyMap());
    }

    @Test
    void 이미_마감된_자료는_취소할_수_없다() {
        AdjustDetail closed = detail(3);
        closed.setAdjustNo(100);
        closed.setBuyAdjustCloseDate("2026-10-01 00:00:00");
        when(mapper.selectDetailForUpdate(9)).thenReturn(closed);

        assertThat(writer.cancelOne(9, "a", "b")).contains("이미 마감된");

        verify(mapper, never()).unmarkShipSelected(anyMap());
        verify(mapper, never()).deleteOpenDetail(any());
    }

    @Test
    void 없는_자료는_사유를_돌려주고_원본_선택일을_풀지_못하면_롤백한다() {
        when(mapper.selectDetailForUpdate(8)).thenReturn(null);
        assertThat(writer.cancelOne(8, "a", "b")).contains("찾을 수 없습니다");

        when(mapper.selectDetailForUpdate(9)).thenReturn(detail(3));
        when(mapper.unmarkShipSelected(anyMap())).thenReturn(0);
        assertThatThrownBy(() -> writer.cancelOne(9, "a", "b")).isInstanceOf(IllegalStateException.class);
        verify(mapper, never()).deleteOpenDetail(any());
    }

    // ───────────────────────── 금액 수정 ─────────────────────────

    private static PriceRequest prices(Long supply, Integer vat) {
        PriceRequest r = new PriceRequest();
        r.setAdjustDetailNo(9);
        r.setSupplyPrice(supply);
        r.setDiscntSupplyPrice(0L);
        r.setBuyPrice(1000L);
        r.setVatRate(vat);
        return r;
    }

    @Test
    void 마감_전이면_보정되고_마감_후_0행이면_false() {
        when(mapper.updatePricesOpen(anyMap())).thenReturn(1, 0);

        assertThat(service.updatePrices(prices(5000L, 10), "a", "b")).isTrue();
        assertThat(service.updatePrices(prices(5000L, 10), "a", "b")).isFalse();
    }

    @Test
    void 반품_자료의_음수_금액은_허용하고_누락_범위_밖은_400() {
        when(mapper.updatePricesOpen(anyMap())).thenReturn(1);
        assertThat(service.updatePrices(prices(-5000L, 10), "a", "b")).isTrue();

        assertThatThrownBy(() -> service.updatePrices(prices(null, 10), "a", "b")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.updatePrices(prices(1_000_000_000_000L, 10), "a", "b")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.updatePrices(prices(5000L, 101), "a", "b")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.updatePrices(prices(5000L, null), "a", "b")).isInstanceOf(ResponseStatusException.class);
    }

    // ───────────────────────── 상점 마감 ─────────────────────────

    private Map<String, Object> shop() {
        Map<String, Object> m = new HashMap<>();
        m.put("shopCode", "S1");
        m.put("supplyCode", "SUP1");
        m.put("svcCode", "SHP001");
        m.put("adjustPeriodCode", "M15");
        return m;
    }

    private void givenSeq(int no) {
        org.mockito.Mockito.doAnswer(inv -> {
            ((AdjustNoSeq) inv.getArgument(0)).setSeq(no);
            return 1;
        }).when(mapper).insertAdjustNo(any(AdjustNoSeq.class));
    }

    @Test
    void 마감은_상점_잠금_채번_정산행_상세_마감_순서로_하고_정산번호를_돌려준다() {
        when(mapper.selectShopForClose("S1")).thenReturn(shop());
        when(mapper.countClosableDetails("S1", "2026-09-01", "2026-09-30")).thenReturn(12);
        givenSeq(1895);
        when(detailMapper.updateShopBuy(anyMap())).thenReturn(12);

        int no = writer.closeShop("S1", "2026-09-01", "2026-09-30", "SHP001", "admin", "관리자");

        assertThat(no).isEqualTo(1895);
        InOrder order = inOrder(mapper, adjustMapper, detailMapper);
        order.verify(mapper).selectShopForClose("S1");
        order.verify(mapper).countClosableDetails("S1", "2026-09-01", "2026-09-30");
        order.verify(mapper).insertAdjustNo(any(AdjustNoSeq.class));
        order.verify(adjustMapper).insert(any(Adjust.class));
        order.verify(detailMapper).updateShopBuy(anyMap());

        ArgumentCaptor<Adjust> adj = ArgumentCaptor.forClass(Adjust.class);
        verify(adjustMapper).insert(adj.capture());
        assertThat(adj.getValue().getAdjustNo()).isEqualTo(1895);
        assertThat(adj.getValue().getShopCode()).isEqualTo("S1");
        assertThat(adj.getValue().getSupplyCode()).isEqualTo("SUP1");
        assertThat(adj.getValue().getAdjustStartDate()).isEqualTo("2026-09-01");
        assertThat(adj.getValue().getAdjustEndDate()).isEqualTo("2026-09-30");
        assertThat(adj.getValue().getAdjustPeriodCode()).isEqualTo("M15");
        assertThat(adj.getValue().getState()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> close = ArgumentCaptor.forClass(Map.class);
        verify(detailMapper).updateShopBuy(close.capture());
        assertThat(close.getValue()).containsEntry("adjustNo", 1895).containsEntry("pShopCode", "S1")
                .containsEntry("pStartDate", "2026-09-01").containsEntry("pEndDate", "2026-09-30");
    }

    @Test
    void 마감할_자료가_없으면_정산번호를_채번하지_않고_끝낸다_번호_소모_없음() {
        when(mapper.selectShopForClose("S1")).thenReturn(shop());
        when(mapper.countClosableDetails("S1", "2026-09-01", "2026-09-30")).thenReturn(0);

        assertThatThrownBy(() -> writer.closeShop("S1", "2026-09-01", "2026-09-30", "SHP001", "a", "b"))
                .isInstanceOf(AdjustDetailWriter.NothingToCloseException.class)
                .hasMessageContaining("마감할 정산자료가 없습니다");

        verify(mapper, never()).insertAdjustNo(any(AdjustNoSeq.class));
        verify(adjustMapper, never()).insert(any());
    }

    @Test
    void 센_뒤_마감_대상이_0건이면_예외로_롤백해_빈_정산_행이_남지_않는다() {
        when(mapper.selectShopForClose("S1")).thenReturn(shop());
        when(mapper.countClosableDetails("S1", "2026-09-01", "2026-09-30")).thenReturn(2);
        givenSeq(1896);
        when(detailMapper.updateShopBuy(anyMap())).thenReturn(0);

        assertThatThrownBy(() -> writer.closeShop("S1", "2026-09-01", "2026-09-30", "SHP001", "a", "b"))
                .isInstanceOf(AdjustDetailWriter.NothingToCloseException.class);
    }

    @Test
    void 없는_상점이나_다른_서비스_상점은_마감하지_않는다() {
        when(mapper.selectShopForClose("NOPE")).thenReturn(null);
        assertThatThrownBy(() -> writer.closeShop("NOPE", "2026-09-01", "2026-09-30", "SHP001", "a", "b"))
                .isInstanceOf(AdjustDetailWriter.NothingToCloseException.class);

        Map<String, Object> other = shop();
        other.put("svcCode", "OTHER");
        when(mapper.selectShopForClose("S2")).thenReturn(other);
        assertThatThrownBy(() -> writer.closeShop("S2", "2026-09-01", "2026-09-30", "SHP001", "a", "b"))
                .hasMessageContaining("다른 서비스");
        verify(adjustMapper, never()).insert(any());
    }

    @Test
    void 마감_요청은_날짜_형식과_순서를_검증하고_중복_상점은_한_번만_처리한다() {
        CloseRequest bad = new CloseRequest();
        bad.setShopCodes(List.of("S1"));
        bad.setStartDate("2026/09/01");
        bad.setEndDate("2026-09-30");
        assertThatThrownBy(() -> service.close(bad, "SHP001", "a", "b")).isInstanceOf(ResponseStatusException.class);

        bad.setStartDate("2026-09-30");
        bad.setEndDate("2026-09-01");
        assertThatThrownBy(() -> service.close(bad, "SHP001", "a", "b")).hasMessageContaining("빠릅니다");

        CloseRequest empty = new CloseRequest();
        empty.setShopCodes(List.of());
        assertThatThrownBy(() -> service.close(empty, "SHP001", "a", "b")).isInstanceOf(ResponseStatusException.class);

        when(mapper.selectShopForClose("S1")).thenReturn(shop());
        when(mapper.countClosableDetails(eq("S1"), any(), any())).thenReturn(3);
        givenSeq(1900);
        when(detailMapper.updateShopBuy(anyMap())).thenReturn(3);
        CloseRequest ok = new CloseRequest();
        ok.setShopCodes(List.of("S1", "S1"));
        ok.setStartDate("2026-09-01");
        ok.setEndDate("2026-09-30");

        BatchResult r = service.close(ok, "SHP001", "admin", "관리자");

        assertThat(r.getSuccess()).isEqualTo(1);
        assertThat(r.getSuccessDetails()).containsExactly("S1 → 정산번호 1900");
        verify(mapper, org.mockito.Mockito.times(1)).selectShopForClose("S1");
    }

    @Test
    void 마감_결과는_상점별로_성공_실패를_집계한다() {
        when(mapper.selectShopForClose("S1")).thenReturn(shop());
        when(mapper.selectShopForClose("S2")).thenReturn(shop());
        when(mapper.countClosableDetails(eq("S1"), any(), any())).thenReturn(5);
        when(mapper.countClosableDetails(eq("S2"), any(), any())).thenReturn(0);
        givenSeq(1901);
        when(detailMapper.updateShopBuy(anyMap())).thenReturn(5, 0);
        CloseRequest req = new CloseRequest();
        req.setShopCodes(List.of("S1", "S2"));
        req.setStartDate("2026-09-01");
        req.setEndDate("2026-09-30");

        BatchResult r = service.close(req, "SHP001", "a", "b");

        assertThat(r.getSuccess()).isEqualTo(1);
        assertThat(r.getFailed()).isEqualTo(1);
        assertThat(r.getFailDetails().get(0)).startsWith("S2 :").contains("마감할 정산자료가 없습니다");
    }

    // ───────────────────────── 컨트롤러: 스코프 계정 차단 / 세션 값 사용 ─────────────────────────

    @Test
    void 상점_채널_제한_계정은_정산_쓰기가_403이고_서비스에_닿지_않는다() {
        AdjustProcessService svc = mock(AdjustProcessService.class);
        DataScopeGuard guard = new DataScopeGuard(mock(com.shopwizard.framework.auth.mapper.DataScopeMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(guard, "mode", "enforce");
        AdjustProcessController controller = new AdjustProcessController(svc, guard);

        MockHttpServletRequest scoped = new MockHttpServletRequest();
        AuthSession.login(scoped, AuthSession.Type.MNGR, Map.of("loginId", "u", "name", "n", "shopCode", "S1"));

        assertThatThrownBy(() -> controller.create(new CreateRequest(), scoped)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller.close(new CloseRequest(), scoped)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller.prices(new PriceRequest(), scoped)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller.cancel(1, scoped)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(svc);
    }

    @Test
    void 제한_없는_관리자는_세션의_처리자와_서비스코드로_마감한다_클라이언트_값은_쓰지_않는다() {
        AdjustProcessService svc = mock(AdjustProcessService.class);
        DataScopeGuard guard = new DataScopeGuard(mock(com.shopwizard.framework.auth.mapper.DataScopeMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(guard, "mode", "enforce");
        AdjustProcessController controller = new AdjustProcessController(svc, guard);
        MockHttpServletRequest admin = new MockHttpServletRequest();
        AuthSession.login(admin, AuthSession.Type.MNGR, Map.of("loginId", "admin1", "name", "관리자1", "svcCode", "SHP001"));
        CloseRequest req = new CloseRequest();

        controller.close(req, admin);

        verify(svc).close(eq(req), eq("SHP001"), eq("admin1"), eq("관리자1"));
    }
}
