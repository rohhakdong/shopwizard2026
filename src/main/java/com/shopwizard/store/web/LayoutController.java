package com.shopwizard.store.web;

import com.shopwizard.store.model.Layout;
import com.shopwizard.store.service.LayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/layout")
@RequiredArgsConstructor
public class LayoutController {
    private final LayoutService layoutService;

    @GetMapping("/list")
    public List<Layout> selectList(@RequestParam Map<String, Object> params) {
        return layoutService.selectList(params);
    }

    @GetMapping("/codemax")
    public String selectLayoutCodeMax(@RequestParam Map<String, Object> params) {
        return layoutService.selectLayoutCodeMax(params);
    }

    @GetMapping
    public Layout select(@RequestParam Map<String, Object> params) {
        return layoutService.select(params);
    }

    @GetMapping("/html")
    public String selectImgHtml(@RequestParam Map<String, Object> params) {
        return layoutService.selectImgHtml(params);
    }

    @PostMapping
    public int insert(@RequestBody Layout layout) {
        return layoutService.insert(layout);
    }

    @PutMapping
    public int update(@RequestBody Layout layout) {
        return layoutService.update(layout);
    }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) {
        return layoutService.delete(params);
    }
}
