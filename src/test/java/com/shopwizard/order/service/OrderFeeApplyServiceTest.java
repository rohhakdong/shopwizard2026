package com.shopwizard.order.service;

import com.shopwizard.order.mapper.OrderFeeApplyMapper;
import com.shopwizard.order.mapper.OrderFeeRuleMapper;
import com.shopwizard.order.model.FeeLine;
import com.shopwizard.order.model.OrderFeeRule;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 수수료 규칙 적용 — 레거시 의미(ⓐ 배송형태 일치 ⓑ 전체무료 ⓒ 수수료율 · 덮어쓰기 · 스마일 · 묶음)와 실행(건별 쓰기). */
class OrderFeeApplyServiceTest {

    private static FeeLine line(int orderNo, String type, int deliFee, int promotFee) {
        FeeLine l = new FeeLine();
        l.setOrderNo(orderNo);
        l.setOrderProdNo(1);
        l.setChnlCode("CH1");
        l.setChnlName("쿠팡");
        l.setProdName("매트 대형");
        l.setItemName("블랙");
        l.setDealName("");
        l.setProdQty(2);
        l.setSalePrice(20000);
        l.setSupplyPrice(15000);
        l.setDeliFeeType(type);
        l.setDeliFeeAmt(deliFee);
        l.setPromotFeeAmt(promotFee);
        l.setRecverAddr("서울 " + orderNo);   // 기본은 서로 다른 수신자
        l.setRecverName("수신자" + orderNo);
        return l;
    }

    private static OrderFeeRule rule(int no, String sect, int deliFee, int promotFee) {
        OrderFeeRule r = new OrderFeeRule();
        r.setRuleNo(no);
        r.setShopCode("S1");
        r.setDeliFeeSect(sect);
        r.setDeliFee(deliFee);
        r.setPromotFee(promotFee);
        r.setChnlFeeRate(BigDecimal.ZERO);
        return r;
    }

    private static OrderFeeLineWriter.Values only(List<OrderFeeApplyService.Planned> plans, int orderNo) {
        return plans.stream().filter(p -> p.line().getOrderNo() == orderNo).findFirst().orElseThrow().values();
    }

    @Test
    void 배송형태가_같은_라인에만_배송비_판촉비를_적용한다() {
        FeeLine prepaid = line(1, "선불", 3000, 0);
        FeeLine free = line(2, "무료", 0, 0);
        OrderFeeRule r = rule(1, "선불", 2500, 500);

        var plans = OrderFeeApplyService.plan(List.of(prepaid, free), List.of(r));

        assertThat(plans).hasSize(1);
        assertThat(only(plans, 1)).isEqualTo(new OrderFeeLineWriter.Values("선불", 2500, 500, 15000));
    }

    @Test
    void 전체무료는_배송형태와_상관없이_적용하고_무료로_바꾼다() {
        FeeLine prepaid = line(1, "선불", 3000, 100);
        FeeLine collect = line(2, "착불", 4000, 0);

        var plans = OrderFeeApplyService.plan(List.of(prepaid, collect), List.of(rule(1, "전체무료", 0, 5000)));

        assertThat(only(plans, 1)).isEqualTo(new OrderFeeLineWriter.Values("무료", 0, 5000, 15000));
        assertThat(only(plans, 2)).isEqualTo(new OrderFeeLineWriter.Values("무료", 0, 5000, 15000));
    }

    @Test
    void 수수료율이_있으면_공급가만_반올림해서_다시_계산한다() {
        FeeLine l = line(1, "선불", 3000, 100);
        l.setSalePrice(10001);
        OrderFeeRule r = rule(1, "전체무료", 999, 999);
        r.setChnlFeeRate(new BigDecimal("9.90"));   // 10001 x 90.1 / 100 = 9010.901 → 9011

        var plans = OrderFeeApplyService.plan(List.of(l), List.of(r));

        assertThat(only(plans, 1)).isEqualTo(new OrderFeeLineWriter.Values("선불", 3000, 100, 9011));   // 배송비·판촉비·배송형태는 그대로
    }

    @Test
    void 나중_규칙이_앞_규칙을_덮어쓰고_앞_규칙이_바꾼_배송형태가_뒤_규칙의_조건이_된다() {
        FeeLine l = line(1, "선불", 3000, 0);
        // 1번: 전체무료 → 무료/0/100, 2번: (이제 무료인) 라인에 무료 규칙 → 0/200, 3번: 선불 규칙은 이미 무료라 안 맞음
        var plans = OrderFeeApplyService.plan(List.of(l),
                List.of(rule(1, "전체무료", 0, 100), rule(2, "무료", 0, 200), rule(3, "선불", 2500, 999)));

        assertThat(only(plans, 1)).isEqualTo(new OrderFeeLineWriter.Values("무료", 0, 200, 15000));
    }

    @Test
    void 샵링커_배송형태_0_1_2_는_무료_선불_착불로_읽고_규칙이_바꾸는_값은_한글이다() {
        FeeLine zero = line(1, "0", 0, 0);       // 무료
        FeeLine one = line(2, "1", 3000, 0);     // 선불
        FeeLine two = line(3, "2", 0, 0);        // 착불

        var plans = OrderFeeApplyService.plan(List.of(zero, one, two), List.of(rule(1, "선불", 2500, 500)));

        assertThat(plans).hasSize(1);
        assertThat(only(plans, 2).deliFeeAmt()).isEqualTo(2500);
        assertThat(OrderFeeApplyService.normType("0")).isEqualTo("무료");
        assertThat(OrderFeeApplyService.normType("2")).isEqualTo("착불");
        assertThat(OrderFeeApplyService.normType(null)).isEmpty();
    }

    @Test
    void 조건은_채널코드_상품명_옵션_딜명_수량_금액_구간이고_0은_제한_없음이다() {
        FeeLine l = line(1, "선불", 3000, 0);
        l.setDealName("여름 특가");
        OrderFeeRule base = rule(1, "전체무료", 0, 100);

        base.setChnlCode("CH2");
        assertThat(OrderFeeApplyService.matches(base, l)).isFalse();
        base.setChnlCode("CH1");
        assertThat(OrderFeeApplyService.matches(base, l)).isTrue();

        base.setProdName("블랙");   // 옵션명도 상품명 검색 대상
        assertThat(OrderFeeApplyService.matches(base, l)).isTrue();
        base.setProdName("없는상품");
        assertThat(OrderFeeApplyService.matches(base, l)).isFalse();
        base.setProdName("");

        base.setDealName("특가");
        assertThat(OrderFeeApplyService.matches(base, l)).isTrue();
        l.setDealName(null);
        assertThat(OrderFeeApplyService.matches(base, l)).isFalse();   // 딜명 조건이 있는데 딜명이 없으면 안 맞는다
        base.setDealName("");

        base.setOrderQtyStart(3);
        assertThat(OrderFeeApplyService.matches(base, l)).isFalse();   // 수량 2
        base.setOrderQtyStart(0);
        base.setOrderQtyEnd(2);
        assertThat(OrderFeeApplyService.matches(base, l)).isTrue();
        base.setOrderQtyEnd(0);
        base.setOrderAmtStart(20001);
        assertThat(OrderFeeApplyService.matches(base, l)).isFalse();   // 금액 20000
        base.setOrderAmtStart(20000);
        base.setOrderAmtEnd(20000);
        assertThat(OrderFeeApplyService.matches(base, l)).isTrue();
    }

    @Test
    void 스마일배송과_11번가풀필먼트_채널은_규칙_결과와_상관없이_선불_0_0이다() {
        FeeLine smile = line(1, "무료", 0, 0);
        smile.setChnlName("G마켓(스마일배송)");
        FeeLine fulfil = line(2, "무료", 0, 0);
        fulfil.setChnlName("11번가풀필먼트");

        var plans = OrderFeeApplyService.plan(List.of(smile, fulfil), List.of(rule(1, "전체무료", 0, 5000)));

        assertThat(only(plans, 1)).isEqualTo(new OrderFeeLineWriter.Values("선불", 0, 0, 15000));
        assertThat(only(plans, 2)).isEqualTo(new OrderFeeLineWriter.Values("선불", 0, 0, 15000));
    }

    @Test
    void 같은_수신자_묶음은_가장_작은_주문번호_외에는_배송비_판촉비가_0이다() {
        FeeLine a = line(10, "선불", 3000, 500);
        FeeLine b = line(11, "선불", 3000, 500);
        FeeLine c = line(12, "선불", 3000, 500);
        a.setRecverAddr("서울 A"); a.setRecverName("홍길동");
        b.setRecverAddr("서울 a "); b.setRecverName("홍길동");   // 대소문자·끝 공백은 같은 수신자
        c.setRecverAddr("부산 B"); c.setRecverName("김철수");

        var plans = OrderFeeApplyService.plan(List.of(a, b, c), List.of(rule(1, "선불", 2500, 500)));

        assertThat(only(plans, 10).deliFeeAmt()).isEqualTo(2500);                       // 가장 작은 주문은 그대로
        assertThat(only(plans, 11)).isEqualTo(new OrderFeeLineWriter.Values("선불", 0, 0, 15000));
        assertThat(plans.stream().filter(p -> p.line().getOrderNo() == 11).findFirst().orElseThrow().bundleZeroed()).isTrue();
        assertThat(only(plans, 12).deliFeeAmt()).isEqualTo(2500);                       // 단독 수신자
    }

    @Test
    void 같은_주문의_여러_라인은_묶음이_아니다() {
        FeeLine a = line(10, "선불", 3000, 500);
        FeeLine b = line(10, "선불", 3000, 500);
        b.setOrderProdNo(2);
        a.setRecverAddr("서울 A"); a.setRecverName("홍길동");
        b.setRecverAddr("서울 A"); b.setRecverName("홍길동");

        var plans = OrderFeeApplyService.plan(List.of(a, b), List.of(rule(1, "선불", 2500, 500)));

        assertThat(plans).hasSize(2);
        assertThat(plans).allSatisfy(p -> assertThat(p.values().deliFeeAmt()).isEqualTo(2500));
    }

    @Test
    void 규칙이_없거나_바뀌는_값이_없으면_아무것도_계획하지_않는다() {
        FeeLine l = line(1, "선불", 2500, 500);
        assertThat(OrderFeeApplyService.plan(List.of(l), List.of())).isEmpty();
        assertThat(OrderFeeApplyService.plan(List.of(), List.of(rule(1, "선불", 2500, 500)))).isEmpty();
        assertThat(OrderFeeApplyService.plan(List.of(l), List.of(rule(1, "선불", 2500, 500)))).isEmpty();   // 이미 같은 값
        assertThat(OrderFeeApplyService.plan(null, null)).isEmpty();
    }

    // ── 실행 ────────────────────────────────────────────────────

    private final OrderFeeApplyMapper applyMapper = mock(OrderFeeApplyMapper.class);
    private final OrderFeeRuleMapper ruleMapper = mock(OrderFeeRuleMapper.class);
    private final OrderFeeLineWriter writer = mock(OrderFeeLineWriter.class);
    private final OrderFeeApplyService service = new OrderFeeApplyService(applyMapper, ruleMapper, writer);

    @Test
    void 바뀐_라인만_건별로_쓰고_건너뛴_건수와_변경_건수를_센다() {
        when(ruleMapper.selectByShop("S1")).thenReturn(List.of(rule(1, "선불", 2500, 500)));
        when(applyMapper.selectPayCompleteLines("S1")).thenReturn(List.of(line(1, "선불", 3000, 0), line(2, "선불", 3000, 0), line(3, "무료", 0, 0)));
        when(writer.write(eq(1), eq(1), eq("S1"), any(), eq("u"), eq("사용자"))).thenReturn(true);
        when(writer.write(eq(2), eq(1), eq("S1"), any(), eq("u"), eq("사용자"))).thenReturn(false);   // 그 사이 출고지시됨

        var result = service.apply(" S1 ", "u", "사용자");

        assertThat(result.rules()).isEqualTo(1);
        assertThat(result.lines()).isEqualTo(3);
        assertThat(result.changed()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        verify(writer, never()).write(eq(3), any(), any(), any(), any(), any());
    }

    @Test
    void 한_건이_예외를_던져도_나머지는_계속하고_건너뜀으로_센다() {
        when(ruleMapper.selectByShop("S1")).thenReturn(List.of(rule(1, "선불", 2500, 500)));
        when(applyMapper.selectPayCompleteLines("S1")).thenReturn(List.of(line(1, "선불", 3000, 0), line(2, "선불", 3000, 0)));
        when(writer.write(eq(1), any(), any(), any(), any(), any())).thenThrow(new RuntimeException("DB"));
        when(writer.write(eq(2), any(), any(), any(), any(), any())).thenReturn(true);

        var result = service.apply("S1", "u", "사용자");

        assertThat(result.changed()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
    }

    @Test
    void 상점이_비어_있으면_400이다() {
        assertThatThrownBy(() -> service.apply("  ", "u", "사용자")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.apply(null, "u", "사용자")).isInstanceOf(ResponseStatusException.class);
    }
}
