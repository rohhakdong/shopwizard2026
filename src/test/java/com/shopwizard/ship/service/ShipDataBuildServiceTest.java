package com.shopwizard.ship.service;

import com.shopwizard.order.mapper.OrderGiftRuleMapper;
import com.shopwizard.order.model.OrderGiftRule;
import com.shopwizard.ship.mapper.ShipAutoOrderMapper;
import com.shopwizard.ship.model.PrintRow;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 배송 데이터 생성 — 사은품 계산(순수 함수)과 실행 순서. */
class ShipDataBuildServiceTest {

    private final ShipAutoOrderMapper mapper = mock(ShipAutoOrderMapper.class);
    private final OrderGiftRuleMapper giftRuleMapper = mock(OrderGiftRuleMapper.class);
    private final ShipDataBuildService service = new ShipDataBuildService(mapper, giftRuleMapper);

    private static PrintRow row(int orderNo, int qty, int salePrice, String addr, String name) {
        PrintRow r = new PrintRow();
        r.setOrderNo(orderNo);
        r.setOrderProdNo(1);
        r.setOrderChangeNo(1);
        r.setOrderType("정상주문");
        r.setChnlCode("CH1");
        r.setOrderProdName("매트 대형 세트");
        r.setDealName("");
        r.setProdName("발주상품");
        r.setProdQty(qty);
        r.setOrderProdQty(qty);
        r.setSalePrice(salePrice);
        r.setRecverAddr(addr);
        r.setRecverName(name);
        r.setPayCmpletDate("2026-11-20 10:00:00");
        return r;
    }

    private static OrderGiftRule rule(int no, int giftQty) {
        OrderGiftRule g = new OrderGiftRule();
        g.setRuleNo(no);
        g.setShopCode("S1");
        g.setGiftProdName1("힣마스크" + no);
        g.setGiftQty(giftQty);
        g.setDupYn(0);
        g.setState(1);
        return g;
    }

    @Test
    void 조건이_없는_규칙은_수신자_그룹마다_사은품_한_건을_가장_앞_주문에_붙인다() {
        List<PrintRow> rows = List.of(row(10, 1, 1000, "서울 A", "홍길동"), row(11, 2, 2000, "서울 A", "홍길동"),
                row(12, 1, 500, "부산 B", "김철수"));

        var plans = ShipDataBuildService.planGifts(rows, List.of(rule(1, 2)));

        assertThat(plans).hasSize(2);
        assertThat(plans.get(0).orderNo()).isEqualTo(10);            // 같은 수신자 두 주문 중 앞 주문
        assertThat(plans.get(0).giftQty()).isEqualTo(2);
        assertThat(plans.get(0).groupQty()).isEqualTo(3);            // 1 + 2
        assertThat(plans.get(0).groupAmt()).isEqualTo(3000);
        assertThat(plans.get(1).orderNo()).isEqualTo(12);
    }

    @Test
    void 총수량_총금액_구간이_맞아야_한다_0은_제한_없음() {
        List<PrintRow> rows = List.of(row(10, 1, 1000, "A", "가"), row(11, 2, 2000, "A", "가"));   // 합 3개 3000원
        OrderGiftRule qty = rule(1, 1); qty.setTotalOrderQtyStart(4);
        OrderGiftRule amt = rule(2, 1); amt.setTotalOrderAmtEnd(2999);
        OrderGiftRule openEnd = rule(3, 1); openEnd.setTotalOrderQtyStart(3); openEnd.setTotalOrderQtyEnd(0);
        OrderGiftRule between = rule(4, 1); between.setTotalOrderQtyStart(2); between.setTotalOrderQtyEnd(3);

        var plans = ShipDataBuildService.planGifts(rows, List.of(qty, amt, openEnd, between));

        assertThat(plans).extracting(ShipDataBuildService.GiftPlan::ruleNo).containsExactly(3, 4);
    }

    @Test
    void 채널_채널상품명_딜명_조건은_그룹_안에_맞는_행이_있어야_하고_앵커는_맞는_행이다() {
        PrintRow plain = row(10, 1, 1000, "A", "가"); plain.setOrderProdName("일반 상품");
        PrintRow mat = row(11, 1, 1000, "A", "가"); mat.setChnlCode("CH2"); mat.setOrderProdName("프리미엄 매트"); mat.setDealName("여름 특가");
        OrderGiftRule r = rule(1, 1);
        r.setChnlCode("CH2"); r.setChnlProdName("매트"); r.setDealName("특가");

        var plans = ShipDataBuildService.planGifts(List.of(plain, mat), List.of(r));

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).orderNo()).isEqualTo(11);   // 조건에 맞는 행
        // 채널이 다르면 안 붙는다
        r.setChnlCode("CH9");
        assertThat(ShipDataBuildService.planGifts(List.of(plain, mat), List.of(r))).isEmpty();
    }

    @Test
    void 채널상품명_포함_비교는_대소문자를_가리지_않는다() {
        PrintRow r1 = row(10, 1, 1000, "A", "가"); r1.setOrderProdName("Premium MAT Large");
        OrderGiftRule r = rule(1, 1); r.setChnlProdName("mat");

        assertThat(ShipDataBuildService.planGifts(List.of(r1), List.of(r))).hasSize(1);
    }

    @Test
    void 증정_기간은_결제완료일시가_기간_안일_때만_맞고_결제일이_없으면_안_맞는다() {
        OrderGiftRule r = rule(1, 1);
        r.setGiftStartDate("2026-11-20 00:00:00");
        r.setGiftEndDate("2026-11-20 23:59:59");
        PrintRow inside = row(10, 1, 1000, "A", "가");               // 11-20 10:00
        PrintRow before = row(11, 1, 1000, "B", "나"); before.setPayCmpletDate("2026-11-19 23:59:59");
        PrintRow after = row(12, 1, 1000, "C", "다"); after.setPayCmpletDate("2026-11-21 00:00:00");
        PrintRow none = row(13, 1, 1000, "D", "라"); none.setPayCmpletDate(null);

        var plans = ShipDataBuildService.planGifts(List.of(inside, before, after, none), List.of(r));

        assertThat(plans).extracting(ShipDataBuildService.GiftPlan::orderNo).containsExactly(10);
    }

    @Test
    void 중복여부_N이면_총수량_N개마다_증정수량을_곱하고_0이면_한_세트() {
        List<PrintRow> rows = List.of(row(10, 5, 5000, "A", "가"));   // 총 5개
        OrderGiftRule perTwo = rule(1, 3); perTwo.setDupYn(2);        // 5 DIV 2 = 2 → 3 x 2 = 6
        OrderGiftRule once = rule(2, 3); once.setDupYn(0);            // 한 세트 = 3

        var plans = ShipDataBuildService.planGifts(rows, List.of(perTwo, once));

        assertThat(plans).extracting(ShipDataBuildService.GiftPlan::giftQty).containsExactly(6, 3);
    }

    @Test
    void 증정수량이_0이_되면_사은품_행을_만들지_않는다() {
        List<PrintRow> rows = List.of(row(10, 1, 1000, "A", "가"));   // 총 1개
        OrderGiftRule perTwo = rule(1, 3); perTwo.setDupYn(2);        // 1 DIV 2 = 0
        OrderGiftRule noName = rule(2, 1); noName.setGiftProdName1(" ");

        assertThat(ShipDataBuildService.planGifts(rows, List.of(perTwo, noName))).isEmpty();
    }

    @Test
    void 규칙이_여럿_맞으면_서로_덮어쓰지_않고_각각_사은품이_된다() {
        List<PrintRow> rows = List.of(row(10, 2, 2000, "A", "가"));

        var plans = ShipDataBuildService.planGifts(rows, List.of(rule(1, 1), rule(2, 2)));

        assertThat(plans).extracting(ShipDataBuildService.GiftPlan::giftName).containsExactly("힣마스크1", "힣마스크2");
    }

    @Test
    void 수신자_비교는_대소문자와_끝_공백을_무시하고_구성_2번_이상_행과_사은품_행은_세지_않는다() {
        PrintRow a = row(10, 1, 1000, "Seoul A ", "Hong");
        PrintRow b = row(11, 1, 1000, "seoul a", "HONG");
        PrintRow part2 = row(10, 5, 9999, "Seoul A ", "Hong"); part2.setOrderChangeNo(2);
        PrintRow gift = row(10, 7, 9999, "Seoul A ", "Hong"); gift.setOrderChangeNo(3); gift.setOrderType("사은품");

        var plans = ShipDataBuildService.planGifts(List.of(a, part2, gift, b), List.of(rule(1, 1)));

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).groupQty()).isEqualTo(2);
        assertThat(plans.get(0).groupAmt()).isEqualTo(2000);
    }

    @Test
    void 규칙이_없거나_행이_없으면_아무것도_만들지_않는다() {
        assertThat(ShipDataBuildService.planGifts(List.of(row(10, 1, 1, "A", "가")), List.of())).isEmpty();
        assertThat(ShipDataBuildService.planGifts(List.of(), List.of(rule(1, 1)))).isEmpty();
        assertThat(ShipDataBuildService.planGifts(null, null)).isEmpty();
    }

    @Test
    void 실행_순서는_지난_사은품_삭제_출고지_사은품_묶음_출력순서이고_출고지가_없으면_기본출고지() {
        when(mapper.selectShopProdDesc("S1")).thenReturn(null);
        when(mapper.selectPendingRows("S1")).thenReturn(List.of(row(10, 1, 1000, "A", "가"), row(11, 1, 1000, "A", "가")));
        when(giftRuleMapper.selectActiveByShop("S1")).thenReturn(List.of(rule(1, 2)));
        when(mapper.insertGiftRow(anyInt(), anyInt(), anyString(), anyInt(), any(), any())).thenReturn(1);
        when(mapper.markBundle("S1")).thenReturn(2);

        var result = service.build(" S1 ", "admin", "관리자");

        var order = inOrder(mapper);
        order.verify(mapper).deletePendingGifts("S1");
        order.verify(mapper).updateShipPlaceAndZip("S1", "기본출고지", "admin", "관리자");
        order.verify(mapper).selectPendingRows("S1");
        order.verify(mapper).updateGiftTotals(10, 1, 2, 2000);
        order.verify(mapper).insertGiftRow(10, 1, "힣마스크1", 2, "admin", "관리자");
        order.verify(mapper).resetBundle("S1");
        order.verify(mapper).markBundle("S1");
        order.verify(mapper).updateSortNames("S1");
        assertThat(result.shipPlace()).isEqualTo("기본출고지");
        assertThat(result.giftRows()).isEqualTo(1);
        assertThat(result.bundledRows()).isEqualTo(2);
        assertThat(result.rows()).isEqualTo(3);   // 대기 행 2 + 사은품 1
    }

    @Test
    void 상점에_출고지가_있으면_그_값을_쓰고_같은_앵커의_사은품이_여럿이어도_총계는_한_번만_기록한다() {
        when(mapper.selectShopProdDesc("S1")).thenReturn(" 본사창고 ");
        when(mapper.selectPendingRows("S1")).thenReturn(List.of(row(10, 2, 2000, "A", "가")));
        when(giftRuleMapper.selectActiveByShop("S1")).thenReturn(List.of(rule(1, 1), rule(2, 1)));
        when(mapper.insertGiftRow(anyInt(), anyInt(), anyString(), anyInt(), any(), any())).thenReturn(1);

        var result = service.build("S1", "admin", "관리자");

        verify(mapper).updateShipPlaceAndZip("S1", "본사창고", "admin", "관리자");
        verify(mapper, times(1)).updateGiftTotals(eq(10), eq(1), eq(2), eq(2000L));
        verify(mapper, times(2)).insertGiftRow(eq(10), eq(1), anyString(), anyInt(), any(), any());
        assertThat(result.giftRows()).isEqualTo(2);
    }

    @Test
    void 사은품_규칙이_없으면_사은품은_없이_묶음과_출력순서만_갱신한다() {
        when(mapper.selectPendingRows("S1")).thenReturn(List.of(row(10, 1, 1000, "A", "가")));
        when(giftRuleMapper.selectActiveByShop("S1")).thenReturn(List.of());

        var result = service.build("S1", "admin", "관리자");

        verify(mapper, never()).insertGiftRow(anyInt(), anyInt(), anyString(), anyInt(), any(), any());
        verify(mapper).markBundle("S1");
        verify(mapper).updateSortNames("S1");
        assertThat(result.giftRows()).isZero();
    }

    @Test
    void 상점이_비어_있으면_400이고_아무것도_건드리지_않는다() {
        assertThatThrownBy(() -> service.build("  ", "admin", "관리자")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.build(null, "admin", "관리자")).isInstanceOf(ResponseStatusException.class);
        verify(mapper, never()).deletePendingGifts(any());
    }
}
