package com.shopwizard.order.service;

import com.shopwizard.order.mapper.KeywordTargetMapper;
import com.shopwizard.order.mapper.OrderKeywrdMapper;
import com.shopwizard.order.model.OrderKeywrd;
import com.shopwizard.order.service.KeywordTargetService.ShopKeywords;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 키워드로 대상 가져오기 — 키워드 판정(순수)과 미리보기·배정. */
class KeywordTargetServiceTest {

    private static OrderKeywrd kw(String shop, String shopName, String sect, String name) {
        OrderKeywrd k = new OrderKeywrd();
        k.setShopCode(shop);
        k.setShopName(shopName);
        k.setKeywrdSect(sect);
        k.setKeywrdName(name);
        k.setState(1);
        return k;
    }

    private static Map<String, Object> line(int orderNo, String prod, String item) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderNo", orderNo);
        m.put("orderProdNo", 1);
        m.put("prodName", prod);
        m.put("itemName", item);
        return m;
    }

    private static ShopKeywords shop(List<OrderKeywrd> list, String code) {
        return KeywordTargetService.keywordsByShop(list).get(code);
    }

    @Test
    void LIKE_는_상품명_LIKE_OPTION_은_옵션명에_포함되면_후보이고_대소문자를_가리지_않는다() {
        ShopKeywords k = shop(List.of(kw("S1", "가", "LIKE", " 매트 "), kw("S1", "가", "LIKE OPTION", "Blue")), "S1");

        assertThat(KeywordTargetService.matches(k, "프리미엄 매트 대형", "")).isTrue();
        assertThat(KeywordTargetService.matches(k, "다른 상품", "색상: BLUE")).isTrue();
        assertThat(KeywordTargetService.matches(k, "다른 상품", "색상: 빨강")).isFalse();
        assertThat(KeywordTargetService.matches(k, "매트에 옵션만", "")).isTrue();   // 앞뒤 공백 제거된 키워드
    }

    @Test
    void NOT_LIKE_NOT_LIKE_OPTION_은_포함되면_후보에서_제외한다() {
        ShopKeywords k = shop(List.of(kw("S1", "가", "LIKE", "매트"), kw("S1", "가", "NOT LIKE", "키친타올"), kw("S1", "가", "NOT LIKE OPTION", "리빙")), "S1");

        assertThat(KeywordTargetService.matches(k, "매트", "")).isTrue();
        assertThat(KeywordTargetService.matches(k, "매트 키친타올 세트", "")).isFalse();
        assertThat(KeywordTargetService.matches(k, "매트", "리빙 타입")).isFalse();
    }

    @Test
    void LIKE_계열_키워드가_없으면_NOT_만_있어도_후보가_없다() {
        ShopKeywords onlyNot = shop(List.of(kw("S1", "가", "NOT LIKE", "키친타올")), "S1");

        assertThat(KeywordTargetService.matches(onlyNot, "아무 상품", "")).isFalse();
        assertThat(onlyNot.total()).isEqualTo(1);
    }

    @Test
    void 비어_있거나_알_수_없는_구분의_키워드는_무시한다() {
        var list = new ArrayList<OrderKeywrd>();
        list.add(kw("S1", "가", "LIKE", "  "));
        list.add(kw("S1", "가", "WEIRD", "x"));
        list.add(kw(null, "가", "LIKE", "x"));

        assertThat(KeywordTargetService.keywordsByShop(list).get("S1").total()).isZero();
        assertThat(KeywordTargetService.keywordsByShop(list)).doesNotContainKey(null);
    }

    // ── 미리보기 · 배정 ─────────────────────────────────────────

    private final KeywordTargetMapper mapper = mock(KeywordTargetMapper.class);
    private final OrderKeywrdMapper keywrdMapper = mock(OrderKeywrdMapper.class);
    private final KeywordAssignWriter writer = mock(KeywordAssignWriter.class);
    private final KeywordTargetService service = new KeywordTargetService(mapper, keywrdMapper, writer);

    private void stub() {
        when(mapper.selectShopName("S1")).thenReturn("상점1");
        when(keywrdMapper.selectAllActive()).thenReturn(List.of(
                kw("S1", "상점1", "LIKE", "매트"), kw("S1", "상점1", "NOT LIKE", "키친타올"),
                kw("S2", "상점2", "LIKE", "매트"), kw("S2", "상점2", "LIKE", "세제")));
        when(mapper.selectUnassignedLines("S1", "1208674775Z2")).thenReturn(List.of(
                line(1, "프리미엄 매트", ""),          // S1·S2 둘 다
                line(2, "매트 키친타올 세트", ""),      // NOT 에 걸려 S1 제외
                line(3, "세탁 세제", ""),              // S2 만
                line(4, "대형 매트 2개", "")));        // S1·S2 둘 다
    }

    @Test
    void 미리보기는_후보_수와_다른_상점_키워드에도_걸린_수와_샘플을_돌려주고_아무것도_쓰지_않는다() {
        stub();

        var p = service.preview(" S1 ");

        assertThat(p.shopName()).isEqualTo("상점1");
        assertThat(p.keywordCnt()).isEqualTo(2);
        assertThat(p.candidates()).isEqualTo(2);          // 주문 1, 4
        assertThat(p.alsoOtherShops()).isEqualTo(2);
        assertThat(p.sample()).hasSize(2);
        assertThat(p.sample().get(0).otherShops()).containsExactly("상점2");
        verify(writer, never()).assign(any(), any(), any(), any(), any(), any());
    }

    @Test
    void 배정은_후보마다_건별로_쓰고_건너뜀과_예외는_세지만_멈추지_않는다() {
        stub();
        when(writer.assign(eq(1), eq(1), eq("S1"), eq("상점1"), eq("u"), eq("사용자"))).thenReturn(true);
        when(writer.assign(eq(4), eq(1), anyString(), anyString(), any(), any())).thenThrow(new RuntimeException("DB"));

        var r = service.assign("S1", "u", "사용자");

        assertThat(r.candidates()).isEqualTo(2);
        assertThat(r.assigned()).isEqualTo(1);
        assertThat(r.skipped()).isEqualTo(1);
        verify(writer, never()).assign(eq(2), anyInt(), any(), any(), any(), any());   // NOT LIKE 라인
        verify(writer, never()).assign(eq(3), anyInt(), any(), any(), any(), any());   // 다른 상점 키워드만
    }

    @Test
    void 키워드가_없는_상점은_후보도_배정도_없다() {
        when(mapper.selectShopName("S9")).thenReturn("상점9");
        when(keywrdMapper.selectAllActive()).thenReturn(List.of(kw("S1", "상점1", "LIKE", "매트")));
        when(mapper.selectUnassignedLines(anyString(), anyString())).thenReturn(List.of(line(1, "매트", "")));

        assertThat(service.preview("S9").candidates()).isZero();
        assertThat(service.preview("S9").keywordCnt()).isZero();
        assertThat(service.assign("S9", "u", "n").assigned()).isZero();
        verify(writer, never()).assign(any(), any(), any(), any(), any(), any());
    }

    @Test
    void 상점이_비었거나_없으면_400이다() {
        assertThatThrownBy(() -> service.preview(" ")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.assign(null, "u", "n")).isInstanceOf(ResponseStatusException.class);
        when(mapper.selectShopName("NOPE")).thenReturn(null);
        assertThatThrownBy(() -> service.preview("NOPE")).isInstanceOf(ResponseStatusException.class);
    }
}
