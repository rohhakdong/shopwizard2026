package com.shopwizard.company.web;

import com.shopwizard.company.model.Shop;
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
