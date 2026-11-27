package com.shopwizard.external.shoplinker;

import java.util.regex.Pattern;

/**
 * 샵링커 송장 호출의 응답. <b>성공 응답의 정확한 형식은 문서·이전 이력 어디에도 없다</b>(레거시는 마지막 {@code >} 뒤 글을 그대로 기록만 하고
 * 성공/실패를 해석하지 않았다). 그래서 상품 호출과 같은 관례로 추정한다: 태그를 걷어낸 본문이 {@code true}·{@code success}·{@code ok}·{@code 성공}
 * 으로 시작하면 성공, 빈 응답이나 그 밖의 글은 실패로 보고 <b>원문을 그대로</b> 이력에 남긴다 — 첫 실전송 1건으로 형식을 확인한다.
 * 잘못 실패로 판정돼도 재전송은 같은 값이라 무해하다.
 */
public record ShoplinkerInvoiceResponse(boolean ok, String message) {

    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern SUCCESS = Pattern.compile("^(?:(?:true|success|ok)\\b|성공).*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static ShoplinkerInvoiceResponse parse(String body) {
        String text = body == null ? "" : TAGS.matcher(body).replaceAll(" ").replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) return new ShoplinkerInvoiceResponse(false, "샵링커 응답이 비어 있습니다.");
        String shown = text.length() > 200 ? text.substring(0, 200) : text;
        return new ShoplinkerInvoiceResponse(SUCCESS.matcher(text).matches(), shown);
    }
}
