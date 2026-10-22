package com.shopwizard.operate.web;

import com.shopwizard.operate.model.EntrstProd;
import com.shopwizard.operate.service.EntrstProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/operate/entrstprod")
public class EntrstProdController {
    private final EntrstProdService entrstProdService;

    @GetMapping("/list")
    public List<EntrstProd> selectList(@RequestParam Map<String, Object> params) { return entrstProdService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return entrstProdService.selectCount(params); }

    @GetMapping
    public EntrstProd select(@RequestParam Map<String, Object> params) { return entrstProdService.select(params); }

}
