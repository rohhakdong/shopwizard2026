package com.shopwizard.store.web;

import com.shopwizard.store.model.StoreImg;
import com.shopwizard.store.service.StoreImgService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/storeimg")
@RequiredArgsConstructor
public class StoreImgController {
    private final StoreImgService storeImgService;

    @GetMapping("/list")
    public List<StoreImg> selectList(@RequestParam Map<String, Object> params) {
        return storeImgService.selectList(params);
    }

    @GetMapping
    public StoreImg select(@RequestParam Map<String, Object> params) {
        return storeImgService.select(params);
    }

    @GetMapping("/html")
    public String selectStoreImgHtml(@RequestParam Map<String, Object> params) {
        return storeImgService.selectStoreImgHtml(params);
    }

    @PostMapping
    public int insert(@RequestBody StoreImg storeImg) {
        return storeImgService.insert(storeImg);
    }

    @PutMapping
    public int update(@RequestBody StoreImg storeImg) {
        return storeImgService.update(storeImg);
    }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) {
        return storeImgService.delete(params);
    }
}
