package com.shopwizard.store.web;

import com.shopwizard.store.model.StoreProd;
import com.shopwizard.store.service.StoreProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/storeprod")
@RequiredArgsConstructor
public class StoreProdController {
    private final StoreProdService storeProdService;

    @GetMapping("/list")
    public List<StoreProd> selectList(@RequestParam Map<String, Object> params) {
        return storeProdService.selectList(params);
    }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) {
        return storeProdService.selectCount(params);
    }

    @GetMapping("/loctype/list")
    public List<StoreProd> selectListLocType(@RequestParam Map<String, Object> params) {
        return storeProdService.selectListLocType(params);
    }

    @GetMapping("/orderqty/list")
    public List<StoreProd> selectListOrderQty(@RequestParam Map<String, Object> params) {
        return storeProdService.selectListOrderQty(params);
    }

    @GetMapping("/storecode")
    public String selectStoreCode(@RequestParam Map<String, Object> params) {
        return storeProdService.selectStoreCode(params);
    }

    @GetMapping
    public StoreProd select(@RequestParam Map<String, Object> params) {
        return storeProdService.select(params);
    }

    @PostMapping
    public int insert(@RequestBody StoreProd storeProd) {
        return storeProdService.insert(storeProd);
    }

    @PutMapping
    public int update(@RequestBody StoreProd storeProd) {
        return storeProdService.update(storeProd);
    }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) {
        return storeProdService.delete(params);
    }

    @DeleteMapping("/prod")
    public int deleteProd(@RequestParam Map<String, Object> params) {
        return storeProdService.deleteProd(params);
    }
}
