package com.shopwizard.external.shoplinker;

import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * 샵링커 송장 등록 호출(GET delivery.php)의 주소를 만든다. 레거시 {@code ShipDirectServiceImpl#runInsertSlnkInvoice} 와 같은 파라미터:
 * {@code customer_id}(협력사 계정) · {@code order_id}(샵링커 주문코드) · {@code delivery_name}(택배사 <b>이름</b> — 코드는 보내지 않는다)
 * · {@code delivery_invoice}(송장번호). 한글은 EUC-KR 로 URL 인코딩한다(샵링커가 받는 인코딩). 순수 함수 — 네트워크를 쓰지 않는다.
 */
public final class ShoplinkerInvoiceRequest {

    public static final Charset EUC_KR = Charset.forName("EUC-KR");
    private static final String MASK = "***";

    private ShoplinkerInvoiceRequest() {}

    /** @param baseUrl 호출 주소(물음표 없음), @param customerId 협력사 계정 ID */
    public record Config(String baseUrl, String customerId) {}

    /** @param url 실제로 호출할 주소(계정 ID 포함), @param logUrl 이력에 남길 주소(계정 ID 가림) */
    public record Built(String url, String logUrl) {}

    public static Built build(Config cfg, String orderCode, String deliveryName, String invNo) {
        if (isBlank(cfg.baseUrl())) throw new IllegalArgumentException("샵링커 송장 주소가 설정되지 않았습니다.");
        if (isBlank(orderCode)) throw new IllegalArgumentException("샵링커 주문코드가 없습니다.");
        if (isBlank(deliveryName)) throw new IllegalArgumentException("택배사 이름이 없습니다.");
        if (isBlank(invNo)) throw new IllegalArgumentException("송장번호가 없습니다.");
        String tail = "&order_id=" + enc(orderCode) + "&delivery_name=" + enc(deliveryName) + "&delivery_invoice=" + enc(invNo);
        String base = cfg.baseUrl().trim() + "?customer_id=";
        String customer = isBlank(cfg.customerId()) ? MASK : enc(cfg.customerId());
        return new Built(base + customer + tail, base + MASK + tail);
    }

    private static String enc(String v) {
        return URLEncoder.encode(v.trim(), EUC_KR);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
