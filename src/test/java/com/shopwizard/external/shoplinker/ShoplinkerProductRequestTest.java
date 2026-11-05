package com.shopwizard.external.shoplinker;

import com.shopwizard.catalog.model.Prod;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShoplinkerProductRequestTest {

    /** 2018~2021 년에 실제로 성공한 호출 로그(tPrdSlinkLog.CallUrl)에서 읽은 파라미터 순서. */
    private static final List<String> LEGACY_ORDER = List.of(
            "brand", "model", "product_name", "product_name2", "image_url", "sale_status",
            "ccategory_l", "ccategory_m", "ccategory_s", "ccategory_d", "customer_id", "maker", "origin",
            "start_price", "market_price", "sale_price", "supply_price", "market_price_p", "sale_price_p", "supply_price_p",
            "step2_price", "step2_sqty", "step2_eqty", "end_sqty", "end_eqty", "quantity",
            "delivery_charge_type", "delivery_charge", "tax_yn", "sale_area", "sex", "partner_product_id",
            "option_name1", "option_value1", "option_name2", "option_value2", "option_name3", "option_value3",
            "descrition", "detail_desc", "add_desc", "bae_desc", "as_desc", "cash_gallery2", "gallery_desc", "cash_count2");

    private static final ShoplinkerProductRequest.Config CFG =
            new ShoplinkerProductRequest.Config("https://slink.test/api.php", "cust-1", "http://www.shopion.co.kr");

    private static Prod prod() {
        Prod p = new Prod();
        p.setProdCode("1000000342");
        p.setShopProdCode("moguchon_0001");
        p.setProdName("가"); p.setProdDsplName("가나");
        p.setBrandName("브랜드"); p.setModelName("M-1");
        p.setCateCode("G009015002000");
        p.setImgUrl("/images/product/a.jpg");
        p.setSaleYn(1); p.setListPrice(26900); p.setSalePrice(26900); p.setSupplyPrice(20000);
        p.setMakerName("제조사"); p.setOriginName("한국");
        p.setDeliFeeType("무료"); p.setVatRate(10);
        return p;
    }

    private static Map<String, String> params(String url) {
        String q = url.substring(url.indexOf('?') + 1);
        Map<String, String> m = new LinkedHashMap<>();
        for (String kv : q.split("&", -1)) {
            int i = kv.indexOf('=');
            m.put(kv.substring(0, i), kv.substring(i + 1));
        }
        return m;
    }

    private static Map<String, String> build(Prod p, String desc, int qty) {
        return params(ShoplinkerProductRequest.build(p, desc, qty, CFG).url());
    }

    @Test
    void 파라미터는_레거시_성공_로그와_같은_46개가_같은_순서() {
        Map<String, String> m = build(prod(), "<p>설명</p>", 10);
        assertEquals(LEGACY_ORDER, new ArrayList<>(m.keySet()));
        assertEquals(46, m.size());
    }

    @Test
    void 한글은_EUC_KR로_URL_인코딩한다() {
        Prod p = prod();
        p.setProdName("가 나");   // 공백 = '+'
        Map<String, String> m = build(p, "가", 1);
        assertEquals("%B0%A1+%B3%AA", m.get("product_name"));    // 가=B0A1, 나=B3AA (EUC-KR)
        assertEquals("%B0%A1+%B3%AA", m.get("descrition"));       // 오타난 파라미터는 상품명과 같은 값
        assertEquals("%B0%A1", m.get("detail_desc"));
    }

    @Test
    void 분류코드는_대중소세로_나누고_000이면_비운다() {
        Map<String, String> m = build(prod(), "", 1);
        assertEquals("G009", m.get("ccategory_l"));
        assertEquals("015", m.get("ccategory_m"));
        assertEquals("002", m.get("ccategory_s"));
        assertEquals("", m.get("ccategory_d"));

        Prod p = prod();
        p.setCateCode("G006008002002");
        Map<String, String> m2 = build(p, "", 1);
        assertEquals("002", m2.get("ccategory_d"));

        p.setCateCode("G000000000000");
        Map<String, String> m3 = build(p, "", 1);
        assertEquals("", m3.get("ccategory_l"));
        assertEquals("", m3.get("ccategory_m"));
    }

    @Test
    void 판매상태_배송비_세율_매핑() {
        Prod p = prod();
        Map<String, String> m = build(p, "", 1);
        assertEquals("001", m.get("sale_status"));
        assertEquals("001", m.get("delivery_charge_type"));
        assertEquals("001", m.get("tax_yn"));

        p.setSaleYn(0); p.setDeliFeeType("착불"); p.setVatRate(0);
        m = build(p, "", 1);
        assertEquals("004", m.get("sale_status"));       // 판매중지/품절/승인대기 = 004
        assertEquals("002", m.get("delivery_charge_type"));
        assertEquals("002", m.get("tax_yn"));             // 면세

        p.setSaleYn(-9); p.setDeliFeeType("선불");
        m = build(p, "", 1);
        assertEquals("004", m.get("sale_status"));
        assertEquals("002", m.get("delivery_charge_type"));
    }

    @Test
    void 가격은_레거시_규칙_start_price는_판매가_100원_아래_음수는_0() {
        Prod p = prod();
        p.setListPrice(30000); p.setSalePrice(26900); p.setSupplyPrice(20000);
        Map<String, String> m = build(p, "", 5);
        assertEquals("26800", m.get("start_price"));
        assertEquals("30000", m.get("market_price"));
        assertEquals("26900", m.get("sale_price"));
        assertEquals("20000", m.get("supply_price"));
        assertEquals("30000", m.get("market_price_p"));
        assertEquals("26900", m.get("sale_price_p"));
        assertEquals("20000", m.get("supply_price_p"));
        assertEquals("5", m.get("quantity"));

        p.setSalePrice(50);
        assertEquals("0", build(p, "", 1).get("start_price"));
    }

    @Test
    void 이미지는_http가_아니면_사이트_주소를_붙이고_URL에_못_쓰는_문자는_인코딩한다() {
        Prod p = prod();
        assertEquals("http://www.shopion.co.kr/images/product/a.jpg", build(p, "", 1).get("image_url"));

        p.setImgUrl("images/product/b.jpg");   // 앞의 / 없어도
        assertEquals("http://www.shopion.co.kr/images/product/b.jpg", build(p, "", 1).get("image_url"));

        p.setImgUrl("https://cdn.example.com/a.jpg");
        assertEquals("https://cdn.example.com/a.jpg", build(p, "", 1).get("image_url"));

        p.setImgUrl("/images/상 품&1.jpg");
        String img = build(p, "", 1).get("image_url");
        assertFalse(img.contains(" "));
        assertFalse(img.contains("&"));
        assertTrue(img.contains("%26"));

        p.setImgUrl("/images/a%20b.jpg");   // 이미 인코딩된 부분은 그대로
        assertTrue(build(p, "", 1).get("image_url").endsWith("/images/a%20b.jpg"));
    }

    @Test
    void 널이_있어도_죽지_않고_빈_값으로_보낸다() {
        Prod p = new Prod();   // 레거시는 deliFeeType/가격이 널이면 NPE
        Map<String, String> m = assertDoesNotThrow(() -> build(p, null, 1));
        assertEquals("", m.get("brand"));
        assertEquals("0", m.get("sale_price"));
        assertEquals("0", m.get("start_price"));
        assertEquals("001", m.get("delivery_charge_type"));   // 미지정 = 무료
        assertEquals("001", m.get("tax_yn"));
        assertEquals("004", m.get("sale_status"));
        assertEquals("", m.get("detail_desc"));
    }

    @Test
    void 계정_ID는_요청에는_실제_값_이력용_주소에는_가려서() {
        ShoplinkerProductRequest.Built b = ShoplinkerProductRequest.build(prod(), "", 1, CFG);
        assertEquals("cust-1", params(b.url()).get("customer_id"));
        assertFalse(b.logUrl().contains("cust-1"));
        assertEquals("***", params(b.logUrl()).get("customer_id"));
        assertTrue(b.logUrl().startsWith("https://slink.test/api.php?"));

        // 계정 ID 가 없어도(시험 모드) 요청은 만들어지고 가려진 값으로 표시된다
        ShoplinkerProductRequest.Built dry = ShoplinkerProductRequest.build(prod(), "", 1,
                new ShoplinkerProductRequest.Config("https://slink.test/api.php", "", "http://x"));
        assertEquals("***", params(dry.url()).get("customer_id"));
    }

    @Test
    void EUC_KR로_표현할_수_없는_문자는_경고() {
        Prod p = prod();
        p.setProdName("좋은 상품 😀");
        ShoplinkerProductRequest.Built b = ShoplinkerProductRequest.build(p, "<p>설명 😀</p>", 1, CFG);
        assertEquals(2, b.warnings().size(), b.warnings().toString());   // 상품명, 상품설명 (같은 상품명 라벨은 한 번만)
        assertTrue(b.warnings().stream().anyMatch(w -> w.contains("상품설명")));
        assertTrue(b.warnings().get(0).contains("EUC-KR"));

        assertTrue(ShoplinkerProductRequest.build(prod(), "일반 한글 설명", 1, CFG).warnings().isEmpty());
    }

    @Test
    void 전송_전_필수값_검사() {
        assertTrue(ShoplinkerProductRequest.validate(prod()).isEmpty());

        Prod p = prod();
        p.setShopProdCode(" "); p.setImgUrl(null); p.setProdName(""); p.setCateCode("G009");
        List<String> errors = ShoplinkerProductRequest.validate(p);
        assertEquals(4, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("상점상품코드"));
        assertTrue(errors.get(1).contains("이미지"));
    }

    // ── 응답 ──────────────────────────────────────────────

    @Test
    void 성공_응답은_샵링커_상품ID를_뽑는다() {
        ShoplinkerProductResponse r = ShoplinkerProductResponse.parse("true|prd149987748|성공\n");
        assertTrue(r.ok());
        assertEquals("prd149987748", r.slinkProdId());
        assertTrue(r.message().contains("prd149987748"));
    }

    @Test
    void 실패_응답과_빈_응답은_실패() {
        ShoplinkerProductResponse f = ShoplinkerProductResponse.parse("false - 상품이미지 누락");
        assertFalse(f.ok());
        assertEquals("false - 상품이미지 누락", f.message());

        ShoplinkerProductResponse empty = ShoplinkerProductResponse.parse("  ");
        assertFalse(empty.ok());
        assertTrue(empty.message().contains("비어"));
        assertFalse(ShoplinkerProductResponse.parse(null).ok());

        assertFalse(ShoplinkerProductResponse.parse("<html>502 Bad Gateway</html>").ok());
        assertEquals(200, ShoplinkerProductResponse.parse("x".repeat(500)).message().length());   // 너무 긴 응답은 자른다
    }

    @Test
    void 성공이지만_ID가_없어도_성공으로_본다() {
        ShoplinkerProductResponse r = ShoplinkerProductResponse.parse("true");
        assertTrue(r.ok());
        assertEquals("", r.slinkProdId());
        assertEquals("성공", r.message());
    }
}
