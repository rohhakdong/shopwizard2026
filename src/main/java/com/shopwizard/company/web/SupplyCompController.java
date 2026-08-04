package com.shopwizard.company.web;

import com.shopwizard.company.model.SupplyComp;
import com.shopwizard.company.service.SupplyCompService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company/supply-comp")
@RequiredArgsConstructor
public class SupplyCompController {
    private final SupplyCompService supplyCompService;

    @GetMapping
    public List<SupplyComp> getList(@RequestParam Map<String, Object> params) {
        return supplyCompService.getList(params);
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params) {
        return supplyCompService.getCount(params);
    }

    @GetMapping("/{supplyCode}")
    public SupplyComp get(@PathVariable String supplyCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("supplyCode", supplyCode);
        return supplyCompService.get(params);
    }

    @GetMapping("/max")
    public String getMax(@RequestParam Map<String, Object> params) {
        return supplyCompService.getMax(params);
    }

    @PostMapping
    public int insert(@RequestBody SupplyComp supplyComp) {
        return supplyCompService.insert(supplyComp);
    }

    @PutMapping
    public int update(@RequestBody SupplyComp supplyComp) {
        return supplyCompService.update(supplyComp);
    }

    @DeleteMapping("/{supplyCode}")
    public int delete(@PathVariable String supplyCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("supplyCode", supplyCode);
        return supplyCompService.delete(params);
    }
}
