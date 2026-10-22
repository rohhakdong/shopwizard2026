package com.shopwizard.company.web;

import com.shopwizard.company.model.SaleComp;
import com.shopwizard.company.service.SaleCompService;
import com.shopwizard.framework.auth.DataScopeGuard;
import jakarta.servlet.http.HttpServletRequest;
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
    private final DataScopeGuard scopeGuard;

    @GetMapping
    public List<SaleComp> getList(@RequestParam Map<String, Object> params) {
        return saleCompService.getList(params);
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params) {
        return saleCompService.getCount(params);
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
    public int insert(@RequestBody SaleComp saleComp, HttpServletRequest request) {
        saleComp.setSvcCode(scopeGuard.svcFor(request, saleComp.getSvcCode()));
        return saleCompService.insert(saleComp);
    }

    @PutMapping
    public int update(@RequestBody SaleComp saleComp, HttpServletRequest request) {
        scopeGuard.requireSaleCompSvc(request, saleComp.getSaleCompCode());
        saleComp.setSvcCode(scopeGuard.svcFor(request, saleComp.getSvcCode()));
        return saleCompService.update(saleComp);
    }

    @DeleteMapping("/{saleCompCode}")
    public int delete(@PathVariable String saleCompCode, HttpServletRequest request) {
        scopeGuard.requireSaleCompSvc(request, saleCompCode);
        Map<String, Object> params = new HashMap<>();
        params.put("saleCompCode", saleCompCode);
        return saleCompService.delete(params);
    }
}
