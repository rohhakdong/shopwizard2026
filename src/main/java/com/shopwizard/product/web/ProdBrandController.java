package com.shopwizard.product.web;

import com.shopwizard.product.model.ProdBrand;
import com.shopwizard.product.service.ProdBrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/prodbrand")
public class ProdBrandController {
    private final ProdBrandService prodBrandService;

    @GetMapping("/list")
    public List<ProdBrand> selectList(@RequestParam Map<String, Object> params) { return prodBrandService.selectList(params); }

    @GetMapping
    public ProdBrand select(@RequestParam Map<String, Object> params) { return prodBrandService.select(params); }

    @PostMapping
    public void insert(@RequestBody ProdBrand prodBrand) { prodBrandService.insert(prodBrand); }

    @PutMapping
    public void update(@RequestBody ProdBrand prodBrand) { prodBrandService.update(prodBrand); }

    @DeleteMapping
    public void delete(@RequestBody Map<String, Object> params) { prodBrandService.delete(params); }
}
