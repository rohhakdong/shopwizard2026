package com.shopwizard.company.web;

import com.shopwizard.company.model.SaleComp;
import com.shopwizard.company.service.SaleCompService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company/sale-comp")
@RequiredArgsConstructor
public class SaleCompController {
    private final SaleCompService saleCompService;

    @GetMapping
    public List<SaleComp> getList(@RequestParam Map<String, Object> params) {
        return saleCompService.getList(params);
    }

    @GetMapping("/{saleCompCode}")
    public SaleComp get(@PathVariable String saleCompCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("saleCompCode", saleCompCode);
        return saleCompService.get(params);
    }

    @GetMapping("/max")
    public String getMax(@RequestParam Map<String, Object> params) {
        return saleCompService.getMax(params);
    }

    @PostMapping
    public int insert(@RequestBody SaleComp saleComp) {
        return saleCompService.insert(saleComp);
    }

    @PutMapping
    public int update(@RequestBody SaleComp saleComp) {
        return saleCompService.update(saleComp);
    }

    @DeleteMapping("/{saleCompCode}")
    public int delete(@PathVariable String saleCompCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("saleCompCode", saleCompCode);
        return saleCompService.delete(params);
    }
}
