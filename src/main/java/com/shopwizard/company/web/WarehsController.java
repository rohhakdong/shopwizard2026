package com.shopwizard.company.web;

import com.shopwizard.company.model.Warehs;
import com.shopwizard.company.service.WarehsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company/warehs")
@RequiredArgsConstructor
public class WarehsController {
    private final WarehsService warehsService;

    @GetMapping
    public List<Warehs> getList(@RequestParam Map<String, Object> params) {
        return warehsService.getList(params);
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params) {
        return warehsService.getCount(params);
    }

    @GetMapping("/{warehsCode}")
    public Warehs get(@PathVariable String warehsCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("warehsCode", warehsCode);
        return warehsService.get(params);
    }

    @PostMapping
    public int insert(@RequestBody Warehs warehs) {
        return warehsService.insert(warehs);
    }

    @PutMapping
    public int update(@RequestBody Warehs warehs) {
        return warehsService.update(warehs);
    }

    @DeleteMapping
    public int delete(@RequestBody Warehs warehs) {
        return warehsService.delete(warehs);
    }
}
