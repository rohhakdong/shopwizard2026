package com.shopwizard.store.web;

import com.shopwizard.store.model.PointProd;
import com.shopwizard.store.service.PointProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/pointprod")
@RequiredArgsConstructor
public class PointProdController {
    private final PointProdService pointProdService;

    @GetMapping("/list")
    public List<PointProd> selectList(@RequestParam Map<String, Object> params) {
        return pointProdService.selectList(params);
    }

    @PostMapping
    public int insert(@RequestBody PointProd pointProd) {
        return pointProdService.insert(pointProd);
    }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) {
        return pointProdService.delete(params);
    }
}
