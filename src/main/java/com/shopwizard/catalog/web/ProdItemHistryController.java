package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.ProdItemHistry;
import com.shopwizard.catalog.service.ProdItemHistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/catalog/prod-item-histry")
@RequiredArgsConstructor
public class ProdItemHistryController {
    private final ProdItemHistryService prodItemHistryService;

    @GetMapping("/list") public List<ProdItemHistry> selectList(@RequestParam Map<String, Object> params) { return prodItemHistryService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return prodItemHistryService.selectCount(params); }
    @GetMapping public ProdItemHistry select(@RequestParam Map<String, Object> params) { return prodItemHistryService.select(params); }
    @PostMapping public int insert(@RequestBody ProdItemHistry prodItemHistry) { return prodItemHistryService.insert(prodItemHistry); }
    @PutMapping public int update(@RequestBody ProdItemHistry prodItemHistry) { return prodItemHistryService.update(prodItemHistry); }
    @DeleteMapping public int delete(@RequestBody ProdItemHistry prodItemHistry) { return prodItemHistryService.delete(prodItemHistry); }
}
