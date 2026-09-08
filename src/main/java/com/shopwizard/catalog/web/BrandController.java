package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.Brand;
import com.shopwizard.catalog.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    /**
     * 프론트의 공통 Api.delete()는 DELETE 요청에 body가 아닌 querystring으로 값을 실어 보내므로
     * (다른 관리 화면들도 대부분 이 방식), @RequestBody 대신 경로변수를 받는다.
     * (이전엔 @RequestBody라 실제로 호출하면 500이 나던 상태였다.)
     */
    @DeleteMapping("/{brandId}")
    public int delete(@PathVariable Integer brandId) {
        Map<String, Object> params = new HashMap<>();
        params.put("brandId", brandId);
        return brandService.delete(params);
    }
}
