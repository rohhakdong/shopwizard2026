package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.Brand;
import com.shopwizard.catalog.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/catalog/brand")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @GetMapping("/list") public List<Brand> selectList(@RequestParam Map<String, Object> params) { return brandService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return brandService.selectCount(params); }
    @GetMapping public Brand select(@RequestParam Map<String, Object> params) { return brandService.select(params); }
    @PostMapping public int insert(@RequestBody Brand brand) { return brandService.insert(brand); }
    @PutMapping public int update(@RequestBody Brand brand) { return brandService.update(brand); }
    @DeleteMapping public int delete(@RequestBody Map<String, Object> params) { return brandService.delete(params); }
}
