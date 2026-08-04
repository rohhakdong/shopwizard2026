package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.ProdItem;
import com.shopwizard.catalog.service.ProdItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController("catalogProdItemController")
@RequestMapping("/catalog/prod-item")
@RequiredArgsConstructor
public class ProdItemController {
    private final ProdItemService prodItemService;

    @GetMapping("/list") public List<ProdItem> selectList(@RequestParam Map<String, Object> params) { return prodItemService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return prodItemService.selectCount(params); }
    @GetMapping public ProdItem select(@RequestParam Map<String, Object> params) { return prodItemService.select(params); }
    @GetMapping("/item-code-by-attr") public String selectItemCodeByAttrVal(@RequestParam Map<String, Object> params) { return prodItemService.selectItemCodeByAttrVal(params); }
    @PostMapping public int insert(@RequestBody ProdItem prodItem) { return prodItemService.insert(prodItem); }
    @PutMapping public int update(@RequestBody ProdItem prodItem) { return prodItemService.update(prodItem); }
    @PutMapping("/itemlist") public int updateItemlist(@RequestBody Map<String, Object> params) { return prodItemService.updateItemlist(params); }
    @DeleteMapping public int delete(@RequestBody ProdItem prodItem) { return prodItemService.delete(prodItem); }
    @PutMapping("/disable/{prodCode}") public int disable(@PathVariable String prodCode) { return prodItemService.disable(prodCode); }
    @PutMapping("/enable") public int enable(@RequestBody Map<String, Object> params) { return prodItemService.enable(params); }
}
