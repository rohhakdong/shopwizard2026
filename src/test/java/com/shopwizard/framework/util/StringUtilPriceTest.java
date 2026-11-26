package com.shopwizard.framework.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 옵션 문구에서 추가금액 읽기 — 레거시 StringUtil 과 같은 결과여야 한다(11번가 "1,000원 2개" 합계가 0 으로 읽히던 이식 오류 회귀). */
class StringUtilPriceTest {

    @Test
    void stripPrice는_괄호_안_추가금액을_읽는다() {
        assertThat(StringUtil.stripPrice("색상:빨강 (+1000원)")).isEqualTo(1000);
        assertThat(StringUtil.stripPrice("(-500원)")).isEqualTo(-500);
        assertThat(StringUtil.stripPrice("옵션 없음")).isZero();
        assertThat(StringUtil.stripPrice("(+1,000원)")).isZero();   // 쉼표 금액은 레거시 패턴이 읽지 못한다
    }

    @Test
    void stripPrices는_금액_x_개수의_합계를_읽는다() {
        assertThat(StringUtil.stripPrices("본품 1,000원 2개")).isEqualTo(2000);
        assertThat(StringUtil.stripPrices("A 500원 3개 B 200원 1개")).isEqualTo(1700);
        assertThat(StringUtil.stripPrices("옵션 없음")).isZero();
    }
}
