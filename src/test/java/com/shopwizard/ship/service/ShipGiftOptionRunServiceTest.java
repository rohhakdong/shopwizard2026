package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.GiftOptionUpdateDetailMapper;
import com.shopwizard.ship.mapper.GiftOptionUpdateMapper;
import com.shopwizard.ship.mapper.ShipPrintMapper;
import com.shopwizard.ship.model.GiftOptionUpdate;
import com.shopwizard.ship.model.GiftOptionUpdateDetail;
import com.shopwizard.ship.model.ShipDirect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 발주내역2생성 — 사은품 문자열·옵션금액 계산(순수), 실행 흐름(진행 락·주문별 결과·실패 처리·무시 규칙). */
class ShipGiftOptionRunServiceTest {

    private GiftOptionUpdateMapper runMapper;
    private GiftOptionUpdateDetailMapper detailMapper;
    private ShipPrintMapper printMapper;
    private ShipGiftOptionRunService service;

    @BeforeEach
    void setUp() {
        runMapper = mock(GiftOptionUpdateMapper.class);
        detailMapper = mock(GiftOptionUpdateDetailMapper.class);
        printMapper = mock(ShipPrintMapper.class);
        when(printMapper.selectShopName("S1")).thenReturn("상점1");
        when(runMapper.selectCount(anyMap())).thenReturn(0);
        when(runMapper.insert(any())).thenAnswer(inv -> {
            ((GiftOptionUpdate) inv.getArgument(0)).setUpdateId(77);
            return 1;
        });
        service = new ShipGiftOptionRunService(runMapper, detailMapper, printMapper, mock(PlatformTransactionManager.class));
    }

    private static ShipDirect line(int prodNo, int changeNo, String type, String prod, int qty, String item, String remark, String orderItem, String chnl) {
        ShipDirect d = new ShipDirect();
        d.setOrderProdNo(prodNo);
        d.setOrderChangeNo(changeNo);
        d.setOrderType(type);
        d.setProdName(prod);
        d.setProdQty(qty);
        d.setItemName(item);
        d.setRemark(remark);
        d.setOrderItemName(orderItem);
        d.setChnlName(chnl);
        return d;
    }

    // ── 계산(순수) ────────────────────────────────────────────

    @Test
    void 사은품_문자열은_힣_행과_사은품_행의_상품명_수량을_쉼표로_잇는다() {
        List<ShipDirect> rows = List.of(
                line(1, 1, "정상주문", "본품", 1, "", null, "", "쿠팡"),
                line(2, 1, "정상주문", "힣사은품A", 1, "", null, "", "쿠팡"),
                line(1, 2, "사은품", "사은품B", 3, "", null, "", "쿠팡"));

        ShipGiftOptionRunService.Plan p = ShipGiftOptionRunService.plan(rows);

        assertThat(p.gift()).isEqualTo("힣사은품A-1,사은품B-3");
        assertThat(p.hasLine()).isTrue();
        assertThat(p.writable()).isTrue();
    }

    @Test
    void 옵션금액은_첫_상품_행의_주문_옵션에서_읽고_11번가는_개수_합계를_더한다() {
        List<ShipDirect> normal = List.of(line(1, 1, "정상주문", "본품", 1, "", null, "색상:빨강 (+1,000원)", "쿠팡"));
        // 쉼표가 든 금액은 레거시 패턴이 읽지 못한다(0) — 괄호 안 숫자만
        assertThat(ShipGiftOptionRunService.plan(normal).optionAmt()).isEqualTo(0);

        List<ShipDirect> plain = List.of(line(1, 1, "정상주문", "본품", 1, "", null, "색상:빨강 (+1000원)", "쿠팡"));
        assertThat(ShipGiftOptionRunService.plan(plain).optionAmt()).isEqualTo(1000);

        List<ShipDirect> minus = List.of(line(1, 1, "정상주문", "본품", 1, "", null, "(-500원)", "쿠팡"));
        assertThat(ShipGiftOptionRunService.plan(minus).optionAmt()).isEqualTo(-500);

        List<ShipDirect> st11 = List.of(line(1, 1, "정상주문", "본품", 1, "", null, "추가 500원 2개 (+300원)", "11번가"));
        assertThat(ShipGiftOptionRunService.plan(st11).optionAmt()).isEqualTo(300 + 1000);
    }

    @Test
    void 구성_행이_여럿이어도_옵션금액은_한_번만_읽고_사은품_행의_옵션문구는_무시한다() {
        List<ShipDirect> rows = List.of(
                line(1, 1, "정상주문", "본품", 1, "", null, "(+1000원)", "쿠팡"),
                line(1, 2, "정상주문", "본품2", 1, "", null, "(+1000원)", "쿠팡"),
                line(1, 3, "사은품", "사은품", 1, "", null, "(+9999원)", "쿠팡"));

        assertThat(ShipGiftOptionRunService.plan(rows).optionAmt()).isEqualTo(1000);
    }

    @Test
    void 속성명에_다른_값이_있으면_쓸_수_없고_이_작업이_쓴_값이면_다시_쓸_수_있다() {
        List<ShipDirect> occupied = List.of(line(1, 1, "정상주문", "본품", 1, "색상:빨강", "", "", "쿠팡"),
                line(1, 2, "사은품", "사은품", 1, "", null, "", "쿠팡"));
        assertThat(ShipGiftOptionRunService.plan(occupied).writable()).isFalse();

        List<ShipDirect> ours = List.of(line(1, 1, "정상주문", "본품", 1, "사은품-1", ShipGiftOptionRunService.MARK, "", "쿠팡"),
                line(1, 2, "사은품", "사은품", 1, "", null, "", "쿠팡"));
        assertThat(ShipGiftOptionRunService.plan(ours).writable()).isTrue();
    }

    @Test
    void 사은품_행의_속성명과_옵션문구는_덮어쓰기_판정과_옵션금액에_쓰지_않는다() {
        List<ShipDirect> rows = List.of(
                line(1, 1, "정상주문", "본품", 1, "", null, "(+700원)", "쿠팡"),
                line(1, 2, "사은품", "사은품", 1, "남아 있던 값", "다른 작업", "(+9999원)", "쿠팡"));

        ShipGiftOptionRunService.Plan p = ShipGiftOptionRunService.plan(rows);

        assertThat(p.writable()).isTrue();
        assertThat(p.optionAmt()).isEqualTo(700);
        assertThat(p.gift()).isEqualTo("사은품-1");
    }

    @Test
    void 사은품도_옵션금액도_없으면_쓸_것이_없다() {
        ShipGiftOptionRunService.Plan p = ShipGiftOptionRunService.plan(List.of(line(1, 1, "정상주문", "본품", 1, "", null, "", "쿠팡")));
        assertThat(p.nothingToWrite()).isTrue();
        assertThat(ShipGiftOptionRunService.plan(List.of()).hasLine()).isFalse();
    }

    // ── 실행 ─────────────────────────────────────────────────

    private void orders(Integer... nos) {
        when(detailMapper.selectRunOrderNos("S1", "2026-11-01", "2026-11-02")).thenReturn(List.of(nos));
    }

    private void printRows(int orderNo, List<ShipDirect> rows) {
        when(detailMapper.selectShipDirectPrintList(argThatOrder(orderNo))).thenReturn(rows);
    }

    private static Map<String, Object> argThatOrder(int orderNo) {
        return org.mockito.ArgumentMatchers.argThat(m -> m != null && Integer.valueOf(orderNo).equals(m.get("orderNo")));
    }

    @Test
    void 실행은_주문별로_성공_무시_오류를_세고_상세를_남기고_성공으로_끝낸다() {
        orders(1, 2, 3);
        printRows(1, List.of(line(1, 1, "정상주문", "본품", 1, "", null, "(+1000원)", "쿠팡"), line(1, 2, "사은품", "사은품", 2, "", null, "", "쿠팡")));
        printRows(2, List.of(line(1, 1, "정상주문", "본품", 1, "", null, "", "쿠팡")));
        printRows(3, List.of());

        ShipGiftOptionRunService.Result r = service.run("S1", "2026-11-01", "2026-11-02", "tmp", "임시");

        assertThat(r.updateId()).isEqualTo(77);
        assertThat(r.resultType()).isEqualTo("성공");
        assertThat(r.total()).isEqualTo(3);
        assertThat(r.success()).isEqualTo(1);
        assertThat(r.ignore()).isEqualTo(1);
        assertThat(r.error()).isEqualTo(1);
        assertThat(r.resultMessag()).isEqualTo("처리를 완료하였습니다. 무시 - 1건, 중복 - 0건, 오류 - 1건, 성공 - 1건");
        assertThat(r.details()).extracting(ShipGiftOptionRunService.Line::resultType).containsExactly("무시", "오류");

        ArgumentCaptor<Map<String, Object>> write = ArgumentCaptor.forClass(Map.class);
        verify(detailMapper, times(1)).updateShipDirectPrint(write.capture());
        assertThat(write.getValue()).containsEntry("orderNo", 1).containsEntry("shopCode", "S1")
                .containsEntry("gift", "사은품-2").containsEntry("optionAmt", 1000).containsEntry("changeId", "tmp");
        ArgumentCaptor<GiftOptionUpdateDetail> detail = ArgumentCaptor.forClass(GiftOptionUpdateDetail.class);
        verify(detailMapper, times(3)).insert(detail.capture());
        assertThat(detail.getAllValues()).extracting(GiftOptionUpdateDetail::getResultType).containsExactly("성공", "무시", "오류");
        assertThat(detail.getAllValues()).extracting(GiftOptionUpdateDetail::getUpdateDetailNo).containsExactly(0, 1, 2);
        assertThat(detail.getAllValues().get(0).getRegistId()).isEqualTo("tmp");

        verify(detailMapper).updatePrintAuctionnoByShop("S1", "2026-11-01", "2026-11-02");
        ArgumentCaptor<GiftOptionUpdate> run = ArgumentCaptor.forClass(GiftOptionUpdate.class);
        verify(runMapper).update(run.capture());
        assertThat(run.getValue().getResultType()).isEqualTo("성공");
        assertThat(run.getValue().getUpdateId()).isEqualTo(77);
    }

    @Test
    void 속성명에_값이_있는_주문은_덮어쓰지_않고_무시로_남긴다() {
        orders(1);
        printRows(1, List.of(line(1, 1, "정상주문", "본품", 1, "색상:빨강", "", "", "쿠팡"), line(1, 2, "사은품", "사은품", 1, "", null, "", "쿠팡")));

        ShipGiftOptionRunService.Result r = service.run("S1", "2026-11-01", "2026-11-02", "tmp", "임시");

        assertThat(r.ignore()).isEqualTo(1);
        verify(detailMapper, never()).updateShipDirectPrint(anyMap());
    }

    @Test
    void 한_주문에서_예외가_나도_오류로_남기고_나머지는_계속_처리한다() {
        orders(1, 2);
        when(detailMapper.selectShipDirectPrintList(argThatOrder(1))).thenThrow(new IllegalStateException("DB 오류"));
        printRows(2, List.of(line(1, 1, "정상주문", "본품", 1, "", null, "", "쿠팡"), line(1, 2, "사은품", "사은품", 1, "", null, "", "쿠팡")));

        ShipGiftOptionRunService.Result r = service.run("S1", "2026-11-01", "2026-11-02", "tmp", "임시");

        assertThat(r.resultType()).isEqualTo("성공");
        assertThat(r.error()).isEqualTo(1);
        assertThat(r.success()).isEqualTo(1);
        assertThat(r.details().get(0).message()).contains("시스템오류").contains("DB 오류");
        ArgumentCaptor<GiftOptionUpdateDetail> detail = ArgumentCaptor.forClass(GiftOptionUpdateDetail.class);
        verify(detailMapper, times(2)).insert(detail.capture());
        assertThat(detail.getAllValues()).extracting(GiftOptionUpdateDetail::getResultType).containsExactly("오류", "성공");
    }

    @Test
    void 바깥에서_실패하면_실행을_실패로_끝낸다() {
        when(detailMapper.selectRunOrderNos(any(), any(), any())).thenThrow(new IllegalStateException("조회 실패"));

        ShipGiftOptionRunService.Result r = service.run("S1", "2026-11-01", "2026-11-02", "tmp", "임시");

        assertThat(r.resultType()).isEqualTo("실패");
        assertThat(r.resultMessag()).contains("조회 실패");
        ArgumentCaptor<GiftOptionUpdate> run = ArgumentCaptor.forClass(GiftOptionUpdate.class);
        verify(runMapper).update(run.capture());
        assertThat(run.getValue().getResultType()).isEqualTo("실패");
    }

    @Test
    void 진행_중인_실행이_있으면_409이고_새_실행을_만들지_않는다() {
        when(runMapper.selectCount(anyMap())).thenReturn(1);

        assertThatThrownBy(() -> service.run("S1", "2026-11-01", "2026-11-02", "tmp", "임시"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("진행");
        verify(runMapper, never()).insert(any());
        verify(runMapper).failStale(ShipGiftOptionRunService.STALE_MINUTES);   // 오래된 진행은 먼저 정리한다
    }

    @Test
    void 입력_검증_상점_기간_31일_상한_없는_상점() {
        assertThatThrownBy(() -> service.run(" ", "2026-11-01", "2026-11-02", "u", "n")).hasMessageContaining("상점");
        assertThatThrownBy(() -> service.run("S1", "", "2026-11-02", "u", "n")).hasMessageContaining("입력");
        assertThatThrownBy(() -> service.run("S1", "2026-11-03", "2026-11-02", "u", "n")).hasMessageContaining("늦습니다");
        assertThatThrownBy(() -> service.run("S1", "2026-10-01", "2026-11-01", "u", "n")).hasMessageContaining("31일");
        assertThat(ShipGiftOptionRunService.range("2026-10-01", "2026-10-31")).containsExactly("2026-10-01", "2026-10-31");
        when(printMapper.selectShopName("NOPE")).thenReturn(null);
        assertThatThrownBy(() -> service.run("NOPE", "2026-11-01", "2026-11-02", "u", "n")).hasMessageContaining("찾을 수 없");
        verify(runMapper, never()).insert(any());
    }

    @Test
    void 이력_상세는_다른_상점의_실행이면_404() {
        GiftOptionUpdate other = new GiftOptionUpdate();
        other.setUpdateId(5);
        other.setRemark("S2");
        when(runMapper.select(anyMap())).thenReturn(other);

        assertThatThrownBy(() -> service.details("S1", 5, 500)).isInstanceOf(ResponseStatusException.class);
        verify(detailMapper, never()).selectList(anyMap());

        other.setRemark("S1");
        when(detailMapper.selectList(anyMap())).thenReturn(List.of(new GiftOptionUpdateDetail()));
        assertThat(service.details("S1", 5, 500)).hasSize(1);

        when(runMapper.selectList(anyMap())).thenReturn(List.of(other));
        assertThat(service.history("S1", 30)).hasSize(1);
        ArgumentCaptor<Map<String, Object>> q = ArgumentCaptor.forClass(Map.class);
        verify(runMapper).selectList(q.capture());
        assertThat(q.getValue()).containsEntry("pRemark", "S1");
    }
}
