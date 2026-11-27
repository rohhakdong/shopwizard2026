package com.shopwizard.external.shoplinker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 샵링커 송장 호출 주소(EUC-KR·계정 ID 마스킹)와 응답 해석. */
class ShoplinkerInvoiceTest {

    private static final ShoplinkerInvoiceRequest.Config CFG = new ShoplinkerInvoiceRequest.Config("https://slink.test/delivery.php", "acct01");

    @Test
    void 주소는_레거시와_같은_파라미터_순서이고_한글은_EUC_KR로_인코딩한다() {
        ShoplinkerInvoiceRequest.Built b = ShoplinkerInvoiceRequest.build(CFG, "ORD-1", "대한통운", " 123 456 ");

        assertThat(b.url()).isEqualTo("https://slink.test/delivery.php?customer_id=acct01&order_id=ORD-1"
                + "&delivery_name=%B4%EB%C7%D1%C5%EB%BF%EE&delivery_invoice=123+456");
    }

    @Test
    void 이력용_주소는_계정_ID를_가린다() {
        ShoplinkerInvoiceRequest.Built b = ShoplinkerInvoiceRequest.build(CFG, "ORD-1", "CJ-GLS택배", "999");

        assertThat(b.logUrl()).contains("customer_id=***").doesNotContain("acct01");
        assertThat(b.url()).contains("customer_id=acct01");
    }

    @Test
    void 계정_ID가_비어도_주소는_만들고_필수값이_없으면_거절한다() {
        assertThat(ShoplinkerInvoiceRequest.build(new ShoplinkerInvoiceRequest.Config("https://x/y.php", " "), "A", "B", "1").url()).contains("customer_id=***");
        assertThatThrownBy(() -> ShoplinkerInvoiceRequest.build(CFG, " ", "B", "1")).hasMessageContaining("주문코드");
        assertThatThrownBy(() -> ShoplinkerInvoiceRequest.build(CFG, "A", "", "1")).hasMessageContaining("택배사");
        assertThatThrownBy(() -> ShoplinkerInvoiceRequest.build(CFG, "A", "B", null)).hasMessageContaining("송장번호");
        assertThatThrownBy(() -> ShoplinkerInvoiceRequest.build(new ShoplinkerInvoiceRequest.Config("", "c"), "A", "B", "1")).hasMessageContaining("주소");
    }

    @Test
    void 응답은_true_success_ok_성공으로_시작하면_성공이고_태그는_걷어낸다() {
        assertThat(ShoplinkerInvoiceResponse.parse("true").ok()).isTrue();
        assertThat(ShoplinkerInvoiceResponse.parse("<html><body>success</body></html>").ok()).isTrue();
        assertThat(ShoplinkerInvoiceResponse.parse("OK|처리").ok()).isTrue();
        assertThat(ShoplinkerInvoiceResponse.parse("성공했습니다").ok()).isTrue();
    }

    @Test
    void 빈_응답_그_밖의_글은_실패이고_원문을_담는다() {
        assertThat(ShoplinkerInvoiceResponse.parse("").ok()).isFalse();
        assertThat(ShoplinkerInvoiceResponse.parse(null).message()).contains("비어");
        ShoplinkerInvoiceResponse r = ShoplinkerInvoiceResponse.parse("false - 주문번호 없음");
        assertThat(r.ok()).isFalse();
        assertThat(r.message()).isEqualTo("false - 주문번호 없음");
        assertThat(ShoplinkerInvoiceResponse.parse("trueish").ok()).isFalse();   // 단어 경계
        assertThat(ShoplinkerInvoiceResponse.parse("x".repeat(500)).message()).hasSize(200);
    }
}
