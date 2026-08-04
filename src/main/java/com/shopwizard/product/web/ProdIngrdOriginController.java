package com.shopwizard.product.web;

import com.shopwizard.product.model.ProdIngrdOrigin;
import com.shopwizard.product.service.ProdIngrdOriginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/prodingrdorigin")
public class ProdIngrdOriginController {
    private final ProdIngrdOriginService prodIngrdOriginService;

    @GetMapping("/list")
    public List<ProdIngrdOrigin> selectList(@RequestParam Map<String, Object> params) { return prodIngrdOriginService.selectList(params); }
}
