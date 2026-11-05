package com.shopwizard.external.shoplinker;

/**
 * 샵링커 상품 호출의 응답. 성공은 {@code true|<샵링커상품ID>|성공}, 실패는 {@code false - <사유>}(예: "false - 상품이미지 누락")
 * 이고, 서버 사정에 따라 빈 응답이 올 때도 있다(2018~2021 년 이력 342건: 성공 332, 실패 10).
 *
 * @param ok            성공 여부
 * @param slinkProdId   성공 시 샵링커가 부여한 상품 ID(없으면 빈 값)
 * @param message       이력/화면에 보여줄 요약
 */
public record ShoplinkerProductResponse(boolean ok, String slinkProdId, String message) {

    public static ShoplinkerProductResponse parse(String body) {
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) return new ShoplinkerProductResponse(false, "", "샵링커 응답이 비어 있습니다.");
        if (text.regionMatches(true, 0, "true", 0, 4)) {
            String[] parts = text.split("\\|", -1);
            String id = parts.length > 1 ? parts[1].trim() : "";
            return new ShoplinkerProductResponse(true, id, id.isEmpty() ? "성공" : "성공 (샵링커 상품ID: " + id + ")");
        }
        return new ShoplinkerProductResponse(false, "", text.length() > 200 ? text.substring(0, 200) : text);
    }
}
