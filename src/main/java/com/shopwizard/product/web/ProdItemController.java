package com.shopwizard.product.web;

import com.shopwizard.product.model.ProdItem;
import com.shopwizard.product.service.ProdItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/proditem")
public class ProdItemController {
    private final ProdItemService prodItemService;

    @GetMapping("/list")
    public List<ProdItem> selectList(@RequestParam Map<String, Object> params) { return prodItemService.selectList(params); }

    @GetMapping
    public ProdItem select(@RequestParam Map<String, Object> params) { return prodItemService.select(params); }
}
