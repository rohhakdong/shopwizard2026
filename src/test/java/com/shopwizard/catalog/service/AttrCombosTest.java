package com.shopwizard.catalog.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 속성값 → 옵션 조합 (레거시 ProdServiceImpl#setProdInfo 규칙). */
class AttrCombosTest {

    @Test
    void 속성값1과_2는_모든_조합이_되고_순서는_레거시_중첩_반복문과_같다() {
        List<String[]> c = AttrCombos.expand("S,M", "빨강,파랑", "", "");

        assertEquals(4, c.size());
        assertArrayEquals(new String[]{"S", "빨강", "", ""}, c.get(0));
        assertArrayEquals(new String[]{"S", "파랑", "", ""}, c.get(1));   // 앞 단계가 바깥 반복
        assertArrayEquals(new String[]{"M", "빨강", "", ""}, c.get(2));
        assertArrayEquals(new String[]{"M", "파랑", "", ""}, c.get(3));
        assertEquals(4, AttrCombos.count("S,M", "빨강,파랑", "", ""));
    }

    @Test
    void 속성값1만_있으면_값마다_옵션_하나() {
        List<String[]> c = AttrCombos.expand("치즈라면밥,육개장라면밥,김치라면밥", null, null, null);

        assertEquals(3, c.size());
        assertArrayEquals(new String[]{"육개장라면밥", "", "", ""}, c.get(1));
    }

    @Test
    void 네_단계_모두_곱한다() {
        List<String[]> c = AttrCombos.expand("a,b", "1,2", "x,y", "가,나");
        assertEquals(16, c.size());
        assertArrayEquals(new String[]{"a", "1", "x", "가"}, c.get(0));
        assertArrayEquals(new String[]{"b", "2", "y", "나"}, c.get(15));
    }

    @Test
    void 공백_빈_값_중복_전각쉼표는_정리한다() {
        assertEquals(List.of("S", "M", "L"), AttrCombos.tokens(" S , ,M,S，L ,"));
        assertEquals(List.of(), AttrCombos.tokens("  ,, "));
        assertEquals(List.of(), AttrCombos.tokens(null));
        assertEquals(3, AttrCombos.expand(" S , ,M,S，L ", "", "", "").size());
    }

    @Test
    void 값이_없으면_조합도_없다() {
        assertEquals(0, AttrCombos.count("", "", "", ""));
        assertEquals(0, AttrCombos.count(null, null, null, null));
        assertTrue(AttrCombos.expand("", "빨강", "", "").isEmpty());   // 속성값1 이 비면 뒤는 모두 무시
    }

    @Test
    void 앞_단계가_비면_뒤_단계는_무시하고_알려_준다() {
        List<String[]> c = AttrCombos.expand("S,M", "", "검정", "");

        assertEquals(2, c.size());
        assertArrayEquals(new String[]{"S", "", "", ""}, c.get(0));   // 속성값3 은 쓰지 않는다(레거시 규칙)
        assertEquals(List.of(3), AttrCombos.ignoredLevels("S,M", "", "검정", ""));
        assertEquals(List.of(3, 4), AttrCombos.ignoredLevels("S", "", "x", "y"));   // 속성값2 가 비어 있어 3·4 모두 무시
    }

    @Test
    void 무시되는_단계가_없으면_빈_목록() {
        assertEquals(List.of(), AttrCombos.ignoredLevels("S", "빨강", "", ""));
        assertEquals(List.of(), AttrCombos.ignoredLevels("S", "빨강", "x", "y"));
        assertEquals(List.of(), AttrCombos.ignoredLevels("", "", "", ""));
    }

    @Test
    void 조합이_100개를_넘으면_만들지_않는다() {
        String ten = "1,2,3,4,5,6,7,8,9,10";

        assertEquals(100, AttrCombos.count(ten, ten, "", ""));
        assertEquals(100, AttrCombos.expand(ten, ten, "", "").size());      // 한도까지는 만든다

        assertEquals(200, AttrCombos.count(ten, ten, "a,b", ""));
        assertTrue(AttrCombos.expand(ten, ten, "a,b", "").isEmpty());          // 넘으면 만들지 않고 호출자가 오류로 안내
        assertEquals(AttrCombos.MAX_COMBOS, 100);
    }

    @Test
    void 조합_수가_아주_커도_계산이_넘치지_않는다() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 400; i++) sb.append(i).append(',');   // 400개 값 × 4단계 = 약 2.6×10^10
        String big = sb.toString();

        assertEquals(Integer.MAX_VALUE, AttrCombos.count(big, big, big, big));   // int 를 넘어도 오버플로 없이 상한으로
        assertTrue(AttrCombos.expand(big, big, big, big).isEmpty());             // 메모리를 쓰지 않고 거절
    }
}
