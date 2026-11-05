package com.shopwizard.external.shoplinker;

/**
 * 샵링커로 나가는 GET 호출. 테스트가 실제 샵링커에 붙지 않도록 인터페이스로 분리했다
 * (운영 샵링커에 상품이 실제로 등록·수정되는 호출이다).
 */
public interface ShoplinkerHttp {
    /**
     * @return 응답 본문을 EUC-KR 로 해석한 문자열(샵링커 응답 인코딩)
     * @throws Exception 연결 실패·타임아웃·HTTP 200 이 아닌 응답
     */
    String get(String url, int timeoutSec) throws Exception;
}
