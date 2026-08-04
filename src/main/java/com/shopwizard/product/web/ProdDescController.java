package com.shopwizard.product.web;

import com.shopwizard.product.model.ProdDesc;
import com.shopwizard.product.service.ProdDescService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/proddesc")
public class ProdDescController {
    private final ProdDescService prodDescService;

    @GetMapping("/list")
    public List<ProdDesc> selectList(@RequestParam Map<String, Object> params) { return prodDescService.selectList(params); }
}
