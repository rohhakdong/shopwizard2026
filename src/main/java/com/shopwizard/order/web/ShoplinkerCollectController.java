package com.shopwizard.order.web;

import com.shopwizard.order.model.ShoplinkerCollectRequest;
import com.shopwizard.order.model.ShoplinkerCollectResult;
import com.shopwizard.order.service.ShoplinkerCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 샵링커 주문수집 실행.
 * 동기 실행 — 응답에 몰별 요약(총/성공/중복/무시/오류)이 담긴다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order/shoplinker")
public class ShoplinkerCollectController {

    private final ShoplinkerCollectService shoplinkerCollectService;

    @PostMapping("/collect")
    public ShoplinkerCollectResult collect(@RequestBody ShoplinkerCollectRequest request) {
        return shoplinkerCollectService.collect(request);
    }
}
