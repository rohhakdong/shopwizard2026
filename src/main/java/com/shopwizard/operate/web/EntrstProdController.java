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

    @PostMapping
    public void insert(@RequestBody EntrstProd entrstProd) { entrstProdService.insert(entrstProd); }

    @PutMapping
    public void update(@RequestBody EntrstProd entrstProd) { entrstProdService.update(entrstProd); }

    @PutMapping("/deli")
    public void updateByDeli(@RequestBody Map<String, Object> params) { entrstProdService.updateByDeli(params); }

    @PutMapping("/price")
    public void updateByPrice(@RequestBody Map<String, Object> params) { entrstProdService.updateByPrice(params); }

    @PutMapping("/bank")
    public void updateByBank(@RequestBody Map<String, Object> params) { entrstProdService.updateByBank(params); }

    @PutMapping("/loan")
    public void updateByLoan(@RequestBody Map<String, Object> params) { entrstProdService.updateByLoan(params); }

    @DeleteMapping
    public void delete(@RequestBody EntrstProd entrstProd) { entrstProdService.delete(entrstProd); }
}
