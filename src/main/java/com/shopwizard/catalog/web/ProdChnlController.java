package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.ProdChnl;
import com.shopwizard.catalog.service.ProdChnlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/catalog/prod-chnl")
@RequiredArgsConstructor
public class ProdChnlController {
    private final ProdChnlService prodChnlService;

    @GetMapping("/list") public List<ProdChnl> selectList(@RequestParam Map<String, Object> params) { return prodChnlService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return prodChnlService.selectCount(params); }
    @GetMapping public ProdChnl select(@RequestParam Map<String, Object> params) { return prodChnlService.select(params); }
}
