package com.shopwizard.product.web;

import com.shopwizard.product.model.ProdDeliFee;
import com.shopwizard.product.service.ProdDeliFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/proddelivee")
public class ProdDeliFeeController {
    private final ProdDeliFeeService prodDeliFeeService;

    @GetMapping("/list")
    public List<ProdDeliFee> selectList(@RequestParam Map<String, Object> params) { return prodDeliFeeService.selectList(params); }

    @GetMapping("/{deliFeeId}")
    public ProdDeliFee select(@PathVariable Integer deliFeeId) { return prodDeliFeeService.select(deliFeeId); }

    @PostMapping
    public void insert(@RequestBody ProdDeliFee prodDeliFee) { prodDeliFeeService.insert(prodDeliFee); }

    @PutMapping
    public void update(@RequestBody ProdDeliFee prodDeliFee) { prodDeliFeeService.update(prodDeliFee); }

    @DeleteMapping("/{deliFeeId}")
    public void delete(@PathVariable Integer deliFeeId) { prodDeliFeeService.delete(deliFeeId); }
}
