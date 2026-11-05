package com.shopwizard.external.shoplinker;

import com.shopwizard.catalog.model.Prod;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 샵링커 상품 등록/수정 호출({@code prodApiInsert_new.php}) 요청 만들기.
 *
 * <p>레거시 {@code ProdServiceImpl#runInsertSlnkProd} 와 <b>같은 파라미터·같은 순서·같은 인코딩</b>(EUC-KR URL 인코딩)으로
 * 만든다 — 2018~2021 년에 실제로 성공한 호출 로그({@code tPrdSlinkLog.CallUrl})와 파라미터 단위로 대조해 확인했다.
 * 샵링커는 공개 문서가 없어 이 레거시 호출이 사실상 명세다. 같은 호출이 등록과 수정을 겸한다(partner_product_id 가 키).</p>
 *
 * <p>레거시와 다른 점(의도적): 널에서 NPE 가 나던 곳을 안전하게 처리하고, start_price 가 음수가 되지 않게 하며,
 * 이미지 주소에 URL 에 쓸 수 없는 문자(공백·한글·&amp; 등)가 있으면 퍼센트 인코딩한다.</p>
 */
public final class ShoplinkerProductRequest {

    /** 샵링커가 받는 인코딩. 한글을 EUC-KR 로 인코딩해야 한다(UTF-8 로 보내면 글자가 깨진다). */
    public static final Charset EUC_KR = Charset.forName("EUC-KR");

    /** 레거시 규칙: 재고가 이 값(또는 없음)이면 무제한으로 보고 10000 을 보낸다. */
    public static final int UNLIMITED_MARK = -1000;
    public static final int UNLIMITED_QTY = 10000;

    private static final String MASK = "***";

    /** @param url 실제로 호출할 주소(계정 ID 포함), @param logUrl 이력에 남길 주소(계정 ID 가림), @param warnings 보내도 되지만 알려 줄 것 */
    public record Built(String url, String logUrl, List<String> warnings) {}

    /** 설정에서 오는 값들. */
    public record Config(String baseUrl, String customerId, String imageBaseUrl) {}

    private ShoplinkerProductRequest() {}

    /** 전송 전 필수값 검사 — 통과하지 못하면 호출하지 않는다(샵링커가 "상품이미지 누락" 등으로 거절하는 것들을 미리 막는다). */
    public static List<String> validate(Prod p) {
        List<String> errors = new ArrayList<>();
        if (isBlank(p.getShopProdCode())) errors.add("상점상품코드가 없습니다(샵링커 상품 식별키라 필수입니다).");
        if (isBlank(p.getImgUrl())) errors.add("대표 이미지가 없습니다.");
        if (isBlank(p.getProdName())) errors.add("상품명이 없습니다.");
        String cate = p.getCateCode();
        if (cate == null || cate.length() != 13) errors.add("분류코드가 13자리가 아닙니다.");
        return errors;
    }

    /**
     * @param quantity 보낼 재고(이미 -1000/없음 규칙이 적용된 값)
     * @param desc     상품설명 HTML
     */
    public static Built build(Prod p, String desc, int quantity, Config cfg) {
        List<String> warnings = new ArrayList<>();
        String customerId = isBlank(cfg.customerId()) ? MASK : URLEncoder.encode(cfg.customerId().trim(), EUC_KR);

        StringBuilder q = new StringBuilder(2048);
        param(q, "brand", enc(p.getBrandName(), "브랜드", warnings));
        param(q, "model", enc(p.getModelName(), "모델", warnings));
        param(q, "product_name", enc(p.getProdName(), "상품명", warnings));
        param(q, "product_name2", enc(p.getProdDsplName(), "상품전시명", warnings));
        param(q, "image_url", imageUrl(p.getImgUrl(), cfg.imageBaseUrl()));
        param(q, "sale_status", Integer.valueOf(1).equals(p.getSaleYn()) ? "001" : "004");

        // 분류코드 'G' + 3자리 x4 → 대/중/소/세분류. 000 이면 그 단계는 비운다(레거시 규칙).
        String cate = p.getCateCode() == null ? "" : p.getCateCode();
        param(q, "ccategory_l", cateSegment(cate, 0, 4, "G000"));
        param(q, "ccategory_m", cateSegment(cate, 4, 7, "000"));
        param(q, "ccategory_s", cateSegment(cate, 7, 10, "000"));
        param(q, "ccategory_d", cateSegment(cate, 10, 13, "000"));

        param(q, "customer_id", customerId);
        param(q, "maker", enc(p.getMakerName(), "제조사", warnings));
        param(q, "origin", enc(p.getOriginName(), "원산지", warnings));

        int sale = nz(p.getSalePrice());
        param(q, "start_price", String.valueOf(Math.max(0, sale - 100)));
        param(q, "market_price", String.valueOf(nz(p.getListPrice())));
        param(q, "sale_price", String.valueOf(sale));
        param(q, "supply_price", String.valueOf(nz(p.getSupplyPrice())));
        param(q, "market_price_p", String.valueOf(nz(p.getListPrice())));
        param(q, "sale_price_p", String.valueOf(sale));
        param(q, "supply_price_p", String.valueOf(nz(p.getSupplyPrice())));
        param(q, "step2_price", "");
        param(q, "step2_sqty", "");
        param(q, "step2_eqty", "");
        param(q, "end_sqty", "");
        param(q, "end_eqty", "");
        param(q, "quantity", String.valueOf(quantity));

        // 배송비 구분: 무료(또는 미지정) = 001, 그 외(선불/착불) = 002
        String deli = p.getDeliFeeType() == null ? "" : p.getDeliFeeType().trim();
        param(q, "delivery_charge_type", deli.isEmpty() || deli.equals("무료") ? "001" : "002");
        param(q, "delivery_charge", "");
        param(q, "tax_yn", Integer.valueOf(0).equals(p.getVatRate()) ? "002" : "001");   // 면세 = 002
        param(q, "sale_area", "001");
        param(q, "sex", "");
        param(q, "partner_product_id", enc(p.getShopProdCode(), "상점상품코드", warnings));

        param(q, "option_name1", enc(p.getAttrName1(), "옵션명1", warnings));
        param(q, "option_value1", enc(p.getAttrValStr1(), "옵션값1", warnings));
        param(q, "option_name2", enc(p.getAttrName2(), "옵션명2", warnings));
        param(q, "option_value2", enc(p.getAttrValStr2(), "옵션값2", warnings));
        param(q, "option_name3", enc(p.getAttrName3(), "옵션명3", warnings));
        param(q, "option_value3", enc(p.getAttrValStr3(), "옵션값3", warnings));

        param(q, "descrition", enc(p.getProdName(), "상품명", warnings));   // 샵링커 쪽 파라미터명 오타 그대로
        param(q, "detail_desc", enc(desc, "상품설명", warnings));
        param(q, "add_desc", "");
        param(q, "bae_desc", enc(p.getDeliFeeName(), "배송비명", warnings));
        param(q, "as_desc", "");
        param(q, "cash_gallery2", "");
        param(q, "gallery_desc", "");
        param(q, "cash_count2", "");

        String query = q.toString();
        String base = cfg.baseUrl() + "?";
        String url = base + query;
        String logUrl = base + query.replace("customer_id=" + customerId, "customer_id=" + MASK);
        return new Built(url, logUrl, dedupe(warnings));
    }

    // ── 내부 ────────────────────────────────────────────────

    private static void param(StringBuilder q, String name, String value) {
        if (q.length() > 0) q.append('&');
        q.append(name).append('=').append(value);
    }

    /** EUC-KR URL 인코딩. 표현할 수 없는 문자는 '?' 로 바뀌므로 경고로 알린다. */
    private static String enc(String s, String label, List<String> warnings) {
        String v = s == null ? "" : s;
        if (!EUC_KR.newEncoder().canEncode(v)) {
            warnings.add(label + "에 샵링커(EUC-KR)가 표현하지 못하는 문자(이모지 등)가 있어 '?' 로 바뀝니다.");
        }
        return URLEncoder.encode(v, EUC_KR);
    }

    private static String cateSegment(String cate, int from, int to, String emptyMark) {
        if (cate.length() < to) return "";
        String seg = cate.substring(from, to);
        return seg.equals(emptyMark) ? "" : seg;
    }

    /** http 로 시작하지 않으면 사이트 주소를 앞에 붙인다(레거시 규칙). URL 에 쓸 수 없는 문자만 퍼센트 인코딩해 이미 인코딩된 부분은 그대로 둔다. */
    static String imageUrl(String img, String imageBase) {
        String v = img == null ? "" : img.trim();
        String full = (v.startsWith("http://") || v.startsWith("https://")) ? v : trimSlash(imageBase) + (v.startsWith("/") ? "" : "/") + v;
        return minimalEncode(full);
    }

    private static String trimSlash(String s) {
        String v = s == null ? "" : s.trim();
        return v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
    }

    private static String minimalEncode(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || "-._~:/?=%+,;@!*'()$".indexOf(c) >= 0;
            if (safe) sb.append((char) c);
            else sb.append('%').append(String.format("%02X", c));
        }
        return sb.toString();
    }

    private static List<String> dedupe(List<String> in) {
        return new ArrayList<>(new java.util.LinkedHashSet<>(in));
    }

    private static int nz(Integer v) { return v == null ? 0 : v; }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
