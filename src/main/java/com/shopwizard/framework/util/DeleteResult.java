package com.shopwizard.framework.util;

/**
 * 상품 삭제 요청의 결과. 주문 이력이 있는 상품은 지우지 않고 완전품절로만 바꾸므로,
 * 화면이 "삭제되었습니다" 와 "삭제하지 않고 품절 처리했습니다" 를 구분해 보여줄 수 있어야 한다.
 *
 * @param deleted 실제로 삭제했으면 true, 삭제 대신 다른 처리를 했으면 false
 * @param message 사용자에게 보여줄 안내(삭제하지 않은 경우에만 채운다)
 */
public record DeleteResult(boolean deleted, String message) {
    public static DeleteResult ofDeleted() { return new DeleteResult(true, null); }
    public static DeleteResult ofSoldOut() {
        return new DeleteResult(false, "주문 이력이 있어 삭제하지 않고 '완전품절'로 처리했습니다.");
    }
}
