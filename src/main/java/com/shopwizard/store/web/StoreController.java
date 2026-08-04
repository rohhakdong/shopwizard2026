package com.shopwizard.store.web;

import com.shopwizard.store.model.Store;
import com.shopwizard.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/store")
@RequiredArgsConstructor
public class StoreController {
    private final StoreService storeService;

    @GetMapping("/list")
    public List<Store> selectList(@RequestParam Map<String, Object> params) {
        return storeService.selectList(params);
    }

    @GetMapping("/codemax")
    public String selectStoreCodeMax(@RequestParam Map<String, Object> params) {
        return storeService.selectStoreCodeMax(params);
    }

    @GetMapping("/table/list")
    public List<Store> selectListTable(@RequestParam Map<String, Object> params) {
        return storeService.selectListTable(params);
    }

    @GetMapping
    public Store select(@RequestParam Map<String, Object> params) {
        return storeService.select(params);
    }

    @GetMapping("/level/list")
    public List<Store> selectListLevel(@RequestParam Map<String, Object> params) {
        return storeService.selectListLevel(params);
    }

    @PostMapping
    public int insert(@RequestBody Store store) {
        return storeService.insert(store);
    }

    @PutMapping
    public int update(@RequestBody Store store) {
        return storeService.update(store);
    }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) {
        return storeService.delete(params);
    }
}
