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

    @DeleteMapping
    public int delete(@RequestBody Shop shop) {
        return shopService.delete(shop);
    }
}
