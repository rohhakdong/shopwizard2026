package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.Prod;
import com.shopwizard.catalog.model.ProdDashBoard;
import com.shopwizard.catalog.service.ProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController("catalogProdController")
@RequestMapping("/catalog/prod")
@RequiredArgsConstructor
public class ProdController {
    private final ProdService prodService;

    @GetMapping("/list") public List<Prod> selectList(@RequestParam Map<String, Object> params) { return prodService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return prodService.selectCount(params); }
    @GetMapping("/{prodCode}") public Prod select(@PathVariable String prodCode) { return prodService.select(prodCode); }
    @GetMapping("/desc/{prodCode}") public String selectProdDesc(@PathVariable String prodCode) { return prodService.selectProdDesc(prodCode); }
    @GetMapping("/code/{shopProdCode}") public String selectProdCode(@PathVariable String shopProdCode) { return prodService.selectProdCode(shopProdCode); }
    @GetMapping("/dashboard") public List<ProdDashBoard> selectListDashboard(@RequestParam Map<String, Object> params) { return prodService.selectListDashboard(params); }
    @PostMapping public int insert(@RequestBody Prod prod) { return prodService.insert(prod); }
    @PutMapping public int update(@RequestBody Prod prod) { return prodService.update(prod); }
    @PutMapping("/price") public int updatePrice(@RequestBody Map<String, Object> params) { return prodService.updatePrice(params); }
    @PutMapping("/approv") public int approv(@RequestBody Map<String, Object> params) { return prodService.approv(params); }
    @DeleteMapping public int delete(@RequestBody Prod prod) { return prodService.delete(prod); }
    @PostMapping("/copy2shopion") public int copy2Shopion(@RequestBody Map<String, Object> params) { return prodService.copy2Shopion(params); }
    @DeleteMapping("/shopion/{prodCode}") public int delete2Shopion(@PathVariable String prodCode) { return prodService.delete2Shopion(prodCode); }
}
