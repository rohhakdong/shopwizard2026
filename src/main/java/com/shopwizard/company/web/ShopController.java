package com.shopwizard.company.web;

import com.shopwizard.company.model.Shop;
import com.shopwizard.company.model.ShopPublicView;
import com.shopwizard.company.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company/shop")
@RequiredArgsConstructor
public class ShopController {
    private final ShopService shopService;

    /**
     * 비회원도 보는 shop.html "전체 쇼핑몰" 드롭다운용 (인증 불필요 — WebMvcConfig 참고).
     * shopCode/shopName만 반환한다. 관리자용 {@link #getList}는 loginId/passwd 등
     * 민감 정보를 포함하므로 절대 이 경로를 관리자 화면에서 재사용하지 말 것.
     */
    @GetMapping("/public")
    public List<ShopPublicView> getPublicList() {
        return shopService.getPublicList();
    }

    /** 관리자 전용 전체 필드 조회 (mngr_loginId 인증 필요 — WebMvcConfig 참고). */
    @GetMapping
    public List<Shop> getList(@RequestParam Map<String, Object> params) {
        return shopService.getList(params);
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params) {
        return shopService.getCount(params);
    }

    @GetMapping("/{shopCode}")
    public Shop get(@PathVariable String shopCode) {
        return shopService.get(shopCode);
    }

    @GetMapping("/max")
    public String getMax(@RequestParam String supplyCode) {
        return shopService.getMax(supplyCode);
    }

    @PostMapping
    public int insert(@RequestBody Shop shop) {
        return shopService.insert(shop);
    }

    @PutMapping
    public int update(@RequestBody Shop shop) {
        return shopService.update(shop);
    }

    /**
     * 프론트의 공통 Api.delete()는 DELETE 요청에 body가 아닌 querystring으로 값을 실어 보내므로
     * (auth-mngr.js 등 기존 화면들도 대부분 이 방식), @RequestBody 대신 경로변수를 받는다.
     * (이전엔 @RequestBody라 실제로 호출하면 500이 나던 상태였다 — company-shop.js의 삭제 버튼이
     * 한 번도 정상 동작한 적이 없었다는 뜻.)
     */
    @DeleteMapping("/{shopCode}")
    public int delete(@PathVariable String shopCode) {
        Shop shop = new Shop();
        shop.setShopCode(shopCode);
        return shopService.delete(shop);
    }
}
