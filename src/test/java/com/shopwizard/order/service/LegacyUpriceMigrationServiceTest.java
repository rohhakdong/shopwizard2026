package com.shopwizard.order.service;

import com.shopwizard.order.mapper.LegacyUpriceMigrationMapper;
import com.shopwizard.order.mapper.UpriceMatchMapper;
import com.shopwizard.order.model.UpriceMatchDetail;
import com.shopwizard.order.service.LegacyUpriceMigrationService.Candidate;
import com.shopwizard.order.service.LegacyUpriceMigrationService.Line;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 레거시 발주단가매칭 이관 — 키 복원·1개분 수량·상태 판정(순수 계획)과 확정(NEW 만, 덮어쓰기 없음). */
class LegacyUpriceMigrationServiceTest {

    private static Map<String, Object> row(int orderNo, int seq, String prod, String item, int orderQty, String code, int totalQty, Integer useYn, String upriceName) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderNo", orderNo);
        m.put("seq", seq);
        m.put("prodName", prod);
        m.put("itemName", item);
        m.put("orderQty", orderQty);
        m.put("code", code);
        m.put("totalQty", totalQty);
        m.put("upriceUseYn", useYn);
        m.put("upriceName", upriceName);
        return m;
    }

    private static Candidate only(List<Candidate> list) {
        assertThat(list).hasSize(1);
        return list.get(0);
    }

    @Test
    void 레거시_라인_합계_수량을_주문수량으로_나눈_상품_1개분_구성이_새_규칙이_된다() {
        // 주문수량 3 → 구성 A 6개(=1개분 2), B 3개(=1개분 1)
        var rows = List.of(row(1, 1, "세트 상품", "옵션A", 3, "A-1", 6, 1, "발주A"), row(1, 2, "세트 상품", "옵션A", 3, "B-1", 3, 1, "발주B"));

        Candidate c = only(LegacyUpriceMigrationService.plan(rows, Map.of(), null));

        assertThat(c.status()).isEqualTo("NEW");
        assertThat(c.prodName()).isEqualTo("세트 상품");
        assertThat(c.itemName()).isEqualTo("옵션A");
        assertThat(c.lines()).containsExactly(new Line("A-1", 2), new Line("B-1", 1));
        assertThat(c.orders()).isEqualTo(1);
    }

    @Test
    void 같은_키가_여러_주문에서_수량만_달라도_1개분이_같으면_하나로_합쳐진다() {
        var rows = new ArrayList<Map<String, Object>>();
        rows.add(row(1, 1, "상품", "", 1, "A-1", 2, 1, "발주A"));
        rows.add(row(2, 1, "상품", "", 3, "A-1", 6, 1, "발주A"));   // 3개 주문, 합계 6 → 1개분 2

        Candidate c = only(LegacyUpriceMigrationService.plan(rows, Map.of(), null));

        assertThat(c.status()).isEqualTo("NEW");
        assertThat(c.orders()).isEqualTo(2);
        assertThat(c.lines()).containsExactly(new Line("A-1", 2));
    }

    @Test
    void 같은_키인데_구성이_다르면_레거시_충돌이고_이관하지_않는다() {
        var rows = List.of(row(1, 1, "상품", "", 1, "A-1", 2, 1, "A"), row(2, 1, "상품", "", 1, "B-1", 2, 1, "B"));

        assertThat(only(LegacyUpriceMigrationService.plan(rows, Map.of(), null)).status()).isEqualTo("CONFLICT_LEGACY");
    }

    @Test
    void 수량이_나누어떨어지지_않거나_주문수량이_0이면_수량_이상이다() {
        assertThat(only(LegacyUpriceMigrationService.plan(List.of(row(1, 1, "상품", "", 3, "A-1", 4, 1, "A")), Map.of(), null)).status()).isEqualTo("ANOMALY_QTY");
        assertThat(only(LegacyUpriceMigrationService.plan(List.of(row(1, 1, "상품", "", 0, "A-1", 4, 1, "A")), Map.of(), null)).status()).isEqualTo("ANOMALY_QTY");
        assertThat(only(LegacyUpriceMigrationService.plan(List.of(row(1, 1, "상품", "", 1, "A-1", 10000, 1, "A")), Map.of(), null)).status()).isEqualTo("ANOMALY_QTY");
    }

    @Test
    void 사용_중지_코드는_검토_필요로_빼고_없는_코드는_코드_없음이다() {
        var inactive = List.of(row(1, 1, "상품", "", 1, "A-1", 1, 0, "A"));
        var missing = List.of(row(1, 1, "상품", "", 1, "NOPE", 1, null, null));
        var blank = List.of(row(1, 1, "상품", "", 1, "", 1, null, null));

        assertThat(only(LegacyUpriceMigrationService.plan(inactive, Map.of(), null)).status()).isEqualTo("INACTIVE_CODE");
        assertThat(only(LegacyUpriceMigrationService.plan(missing, Map.of(), null)).status()).isEqualTo("MISSING_CODE");
        assertThat(only(LegacyUpriceMigrationService.plan(blank, Map.of(), null)).status()).isEqualTo("MISSING_CODE");
    }

    @Test
    void 같은_코드는_합산하고_30줄이_넘으면_제외한다() {
        var dup = List.of(row(1, 1, "상품", "", 1, "A-1", 1, 1, "A"), row(1, 2, "상품", "", 1, "A-1", 2, 1, "A"));
        assertThat(only(LegacyUpriceMigrationService.plan(dup, Map.of(), null)).lines()).containsExactly(new Line("A-1", 3));

        var many = new ArrayList<Map<String, Object>>();
        for (int i = 1; i <= 31; i++) many.add(row(1, i, "상품", "", 1, "C-" + i, 1, 1, "C"));
        assertThat(only(LegacyUpriceMigrationService.plan(many, Map.of(), null)).status()).isEqualTo("TOO_MANY");
    }

    @Test
    void 이미_있는_규칙은_같으면_SAME_다르면_기존과_충돌이고_덮어쓰지_않는다() {
        var rows = List.of(row(1, 1, "상품", "", 1, "A-1", 2, 1, "A"));

        assertThat(only(LegacyUpriceMigrationService.plan(rows, Map.of("상품\u0000", List.of(new Line("A-1", 2))), null)).status()).isEqualTo("SAME");
        assertThat(only(LegacyUpriceMigrationService.plan(rows, Map.of("상품\u0000", List.of(new Line("A-1", 5))), null)).status()).isEqualTo("CONFLICT_EXISTING");
    }

    @Test
    void 앞뒤_공백은_출고지시가_조회하는_방식대로_잘라_키로_쓰고_표시한다() {
        var rows = List.of(row(1, 1, "  상품 ", " 옵션 ", 1, "A-1", 1, 1, "A"));

        Candidate c = only(LegacyUpriceMigrationService.plan(rows, Map.of(), null));

        assertThat(c.prodName()).isEqualTo("상품");
        assertThat(c.itemName()).isEqualTo("옵션");
        assertThat(c.keyTrimmed()).isTrue();
    }

    @Test
    void 사은품_이름_구성은_일반_줄로_이관하되_개수를_알린다_상품명_시작으로_범위를_좁힌다() {
        var rows = List.of(row(1, 1, "마스크 세트", "", 1, "A-1", 1, 1, "발주A"), row(1, 2, "마스크 세트", "", 1, "G-1", 1, 1, "힣마스크"),
                row(2, 1, "다른 상품", "", 1, "A-1", 1, 1, "발주A"));

        var all = LegacyUpriceMigrationService.plan(rows, Map.of(), null);
        var filtered = LegacyUpriceMigrationService.plan(rows, Map.of(), "마스크");

        assertThat(all).hasSize(2);
        Candidate c = only(filtered);
        assertThat(c.status()).isEqualTo("NEW");
        assertThat(c.lines()).hasSize(2);   // 힣 줄도 포함
        assertThat(c.giftLines()).isEqualTo(1);
    }

    // ── 확정 ────────────────────────────────────────────────────

    private final LegacyUpriceMigrationMapper mapper = mock(LegacyUpriceMigrationMapper.class);
    private final UpriceMatchMapper upriceMapper = mock(UpriceMatchMapper.class);
    private final LegacyUpriceMigrationWriter writer = new LegacyUpriceMigrationWriter(mapper, upriceMapper);
    private final LegacyUpriceMigrationService service = new LegacyUpriceMigrationService(mapper, writer);

    @Test
    void 확정은_NEW_만_저장하고_표식_등록자와_비고로_남긴다() {
        when(mapper.selectCandidates()).thenReturn(List.of(
                row(7, 1, "새 상품", "", 2, "A-1", 4, 1, "A"),
                row(7, 2, "새 상품", "", 2, "B-1", 2, 1, "B"),
                row(8, 1, "중지 상품", "", 1, "X-1", 1, 0, "X")));
        when(mapper.selectExistingRules()).thenReturn(List.of());
        when(mapper.countKey(anyString(), anyString())).thenReturn(0);

        var result = service.commit(null, "관리자");

        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.counts()).containsEntry("NEW", 1).containsEntry("INACTIVE_CODE", 1);
        ArgumentCaptor<UpriceMatchDetail> cap = ArgumentCaptor.forClass(UpriceMatchDetail.class);
        verify(upriceMapper, times(2)).insertDetail(cap.capture());
        UpriceMatchDetail first = cap.getAllValues().get(0);
        assertThat(first.getProdName()).isEqualTo("새 상품");
        assertThat(first.getMatchSeq()).isEqualTo(1);
        assertThat(first.getMatchUpriceCode()).isEqualTo("A-1");
        assertThat(first.getQty()).isEqualTo(2);
        assertThat(first.getRegistId()).isEqualTo("MIGRATE-LEGACY");
        assertThat(first.getRegistName()).isEqualTo("관리자");
        assertThat(first.getRemark()).isEqualTo("LEG:7");
        assertThat(cap.getAllValues().get(1).getMatchSeq()).isEqualTo(2);
    }

    @Test
    void 그_사이_같은_키의_규칙이_생겼으면_저장하지_않고_건너뛰며_한_건_실패가_나머지를_막지_않는다() {
        when(mapper.selectCandidates()).thenReturn(List.of(
                row(1, 1, "먼저 생김", "", 1, "A-1", 1, 1, "A"),
                row(2, 1, "실패함", "", 1, "A-1", 1, 1, "A"),
                row(3, 1, "정상", "", 1, "A-1", 1, 1, "A")));
        when(mapper.selectExistingRules()).thenReturn(List.of());
        when(mapper.countKey("먼저 생김", "")).thenReturn(1);
        when(mapper.countKey("실패함", "")).thenReturn(0);
        when(mapper.countKey("정상", "")).thenReturn(0);
        org.mockito.Mockito.doThrow(new RuntimeException("DB")).when(upriceMapper)
                .insertDetail(org.mockito.ArgumentMatchers.argThat(d -> d != null && "실패함".equals(d.getProdName())));

        var result = service.commit(null, null);

        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        verify(upriceMapper, never()).insertDetail(org.mockito.ArgumentMatchers.argThat(d -> d != null && "먼저 생김".equals(d.getProdName())));
    }

    @Test
    void 미리보기는_상태별_건수와_복원하지_못한_레거시_규칙을_돌려주고_아무것도_쓰지_않는다() {
        when(mapper.selectCandidates()).thenReturn(List.of(row(1, 1, "상품", "", 1, "A-1", 1, 1, "A")));
        when(mapper.selectExistingRules()).thenReturn(List.of());
        when(mapper.countLegacyHeaders()).thenReturn(76);
        when(mapper.selectUnmappableHeaders()).thenReturn(List.of(Map.of("orderNo", 5)));

        var preview = service.preview(null);

        assertThat(preview.legacyHeaders()).isEqualTo(76);
        assertThat(preview.keys()).isEqualTo(1);
        assertThat(preview.counts()).containsEntry("NEW", 1);
        assertThat(preview.unmappable()).hasSize(1);
        verify(upriceMapper, never()).insertDetail(any());
        verify(mapper, never()).countKey(anyString(), anyString());
    }
}
