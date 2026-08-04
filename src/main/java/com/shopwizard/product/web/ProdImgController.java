package com.shopwizard.product.web;

import com.shopwizard.product.model.ProdImg;
import com.shopwizard.product.service.ProdImgService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/prodimg")
public class ProdImgController {
    private final ProdImgService prodImgService;

    @GetMapping("/list")
    public List<ProdImg> selectList(@RequestParam Map<String, Object> params) { return prodImgService.selectList(params); }
}
