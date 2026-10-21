package com.shopwizard.product.web;

import com.shopwizard.product.model.Prod;
import com.shopwizard.product.service.ProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/prod")
public class ProdController {
    private final ProdService prodService;

    @GetMapping("/list")
    public List<Prod> selectList(@RequestParam Map<String, Object> params) { return prodService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return prodService.selectCount(params); }

    @GetMapping
    public Prod select(@RequestParam Map<String, Object> params) { return prodService.select(params); }

    @GetMapping("/byshopprodcode")
    public Prod selectByShopProdCode(@RequestParam Map<String, Object> params) { return prodService.selectByShopProdCode(params); }

    @GetMapping("/shopprodcode")
    public String selectShopProdCode(@RequestParam String shopProdCode) { return prodService.selectShopProdCode(shopProdCode); }

    @DeleteMapping
    public void delete(@RequestParam String prodCode) { prodService.delete(prodCode); }

    @GetMapping("/search/list")
    public List<Prod> selectListSearch(@RequestParam Map<String, Object> params) { return prodService.selectListSearch(params); }

    @GetMapping("/search/count")
    public int selectCountSearch(@RequestParam Map<String, Object> params) { return prodService.selectCountSearch(params); }

    @GetMapping("/keyword/list")
    public List<Prod> selectListKeyword(@RequestParam Map<String, Object> params) { return prodService.selectListKeyword(params); }

    @GetMapping("/brand/list")
    public List<Prod> selectListBrand(@RequestParam Map<String, Object> params) { return prodService.selectListBrand(params); }

    @GetMapping("/supplyqty")
    public int selectSupplyQty(@RequestParam Map<String, Object> params) { return prodService.selectSupplyQty(params); }

    @GetMapping("/supplyqty/item")
    public int selectSupplyQtyItem(@RequestParam Map<String, Object> params) { return prodService.selectSupplyQtyItem(params); }

    @PutMapping("/supplyqty")
    public void updateSupplyQty(@RequestBody Map<String, Object> params) { prodService.updateSupplyQty(params); }

    @PutMapping("/saleyn")
    public void updateSaleYn(@RequestBody Map<String, Object> params) { prodService.updateSaleYn(params); }

    @GetMapping("/naver/list")
    public List<Map<String, Object>> selectListNaver(@RequestParam Map<String, Object> params) { return prodService.selectListNaver(params); }
}
