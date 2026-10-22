package com.shopwizard.company.web;

import com.shopwizard.company.model.Comp;
import com.shopwizard.company.service.CompService;
import com.shopwizard.framework.auth.DataScopeGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("companyCompController")
@RequestMapping("/company/comp")
@RequiredArgsConstructor
public class CompController {
    private final CompService compService;
    private final DataScopeGuard scopeGuard;

    @GetMapping
    public List<Comp> getList(@RequestParam Map<String, Object> params) {
        return compService.getList(params);
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params) {
        return compService.getCount(params);
    }

    @GetMapping("/{compCode}")
    public Comp get(@PathVariable String compCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("compCode", compCode);
        return compService.get(params);
    }

    @PostMapping
    public int insert(@RequestBody Comp comp, HttpServletRequest request) {
        comp.setSvcCode(scopeGuard.svcFor(request, comp.getSvcCode()));
        return compService.insert(comp);
    }

    @PutMapping
    public int update(@RequestBody Comp comp, HttpServletRequest request) {
        scopeGuard.requireCompSvc(request, comp.getCompCode());
        comp.setSvcCode(scopeGuard.svcFor(request, comp.getSvcCode()));
        return compService.update(comp);
    }

    @DeleteMapping("/{compCode}")
    public int delete(@PathVariable String compCode, HttpServletRequest request) {
        scopeGuard.requireCompSvc(request, compCode);
        Map<String, Object> params = new HashMap<>();
        params.put("compCode", compCode);
        return compService.delete(params);
    }
}
