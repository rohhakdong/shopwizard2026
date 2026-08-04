package com.shopwizard.company.web;

import com.shopwizard.company.model.Comp;
import com.shopwizard.company.service.CompService;
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
    public int insert(@RequestBody Comp comp) {
        return compService.insert(comp);
    }

    @PutMapping
    public int update(@RequestBody Comp comp) {
        return compService.update(comp);
    }

    @DeleteMapping("/{compCode}")
    public int delete(@PathVariable String compCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("compCode", compCode);
        return compService.delete(params);
    }
}
