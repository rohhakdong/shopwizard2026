package com.shopwizard.store.web;

import com.shopwizard.store.model.LayoutProd;
import com.shopwizard.store.service.LayoutProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/layoutprod")
@RequiredArgsConstructor
public class LayoutProdController {
    private final LayoutProdService layoutProdService;

    @GetMapping("/list")
    public List<LayoutProd> selectList(@RequestParam Map<String, Object> params) {
        return layoutProdService.selectList(params);
    }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) {
        return layoutProdService.selectCount(params);
    }

    @PostMapping
    public int insert(@RequestBody LayoutProd layoutProd) {
        return layoutProdService.insert(layoutProd);
    }

    @PutMapping
    public int update(@RequestBody LayoutProd layoutProd) {
        return layoutProdService.update(layoutProd);
    }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) {
        return layoutProdService.delete(params);
    }
}
