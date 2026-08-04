package com.shopwizard.company.web;

import com.shopwizard.company.model.ShopWarehs;
import com.shopwizard.company.service.ShopWarehsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company/shop-warehs")
@RequiredArgsConstructor
public class ShopWarehsController {
    private final ShopWarehsService shopWarehsService;

    @GetMapping
    public List<ShopWarehs> getList(@RequestParam Map<String, Object> params) {
        return shopWarehsService.getList(params);
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params) {
        return shopWarehsService.getCount(params);
    }

    @GetMapping("/one")
    public ShopWarehs get(@RequestParam Map<String, Object> params) {
        return shopWarehsService.get(params);
    }

    @PostMapping
    public int insert(@RequestBody ShopWarehs shopWarehs) {
        return shopWarehsService.insert(shopWarehs);
    }

    @PutMapping
    public int update(@RequestBody ShopWarehs shopWarehs) {
        return shopWarehsService.update(shopWarehs);
    }

    @DeleteMapping
    public int delete(@RequestBody ShopWarehs shopWarehs) {
        return shopWarehsService.delete(shopWarehs);
    }
}
