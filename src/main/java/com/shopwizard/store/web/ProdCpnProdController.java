package com.shopwizard.store.web;

import com.shopwizard.store.model.ProdCpnProd;
import com.shopwizard.store.service.ProdCpnProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/prodcpnprod")
@RequiredArgsConstructor
public class ProdCpnProdController {
    private final ProdCpnProdService prodCpnProdService;

    @GetMapping("/list")
    public List<ProdCpnProd> selectList(@RequestParam Map<String, Object> params) {
        return prodCpnProdService.selectList(params);
    }

    @PostMapping
    public int insert(@RequestBody ProdCpnProd prodCpnProd) {
        return prodCpnProdService.insert(prodCpnProd);
    }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) {
        return prodCpnProdService.delete(params);
    }
}
