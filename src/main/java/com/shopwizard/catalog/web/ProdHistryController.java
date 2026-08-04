package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.ProdHistry;
import com.shopwizard.catalog.service.ProdHistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/catalog/prod-histry")
@RequiredArgsConstructor
public class ProdHistryController {
    private final ProdHistryService prodHistryService;

    @GetMapping("/list") public List<ProdHistry> selectList(@RequestParam Map<String, Object> params) { return prodHistryService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return prodHistryService.selectCount(params); }
    @GetMapping public ProdHistry select(@RequestParam Map<String, Object> params) { return prodHistryService.select(params); }
    @PostMapping public int insert(@RequestBody ProdHistry prodHistry) { return prodHistryService.insert(prodHistry); }
    @PutMapping public int update(@RequestBody ProdHistry prodHistry) { return prodHistryService.update(prodHistry); }
    @DeleteMapping public int delete(@RequestBody ProdHistry prodHistry) { return prodHistryService.delete(prodHistry); }
}
