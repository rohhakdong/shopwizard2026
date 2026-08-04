package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.ProdImg;
import com.shopwizard.catalog.service.ProdImgService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController("catalogProdImgController")
@RequestMapping("/catalog/prod-img")
@RequiredArgsConstructor
public class ProdImgController {
    private final ProdImgService prodImgService;

    @GetMapping("/list") public List<ProdImg> selectList(@RequestParam Map<String, Object> params) { return prodImgService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return prodImgService.selectCount(params); }
    @GetMapping public ProdImg select(@RequestParam Map<String, Object> params) { return prodImgService.select(params); }
    @GetMapping("/max-code/{prodCode}") public String selectMaxCode(@PathVariable String prodCode) { return prodImgService.selectMaxCode(prodCode); }
    @PostMapping public int insert(@RequestBody ProdImg prodImg) { return prodImgService.insert(prodImg); }
    @PutMapping public int update(@RequestBody ProdImg prodImg) { return prodImgService.update(prodImg); }
    @DeleteMapping public int delete(@RequestBody ProdImg prodImg) { return prodImgService.delete(prodImg); }
}
