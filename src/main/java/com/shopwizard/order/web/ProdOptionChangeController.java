package com.shopwizard.order.web;

import com.shopwizard.order.model.ProdOptionChange;
import com.shopwizard.order.service.ProdOptionChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/prod-option-change")
@RequiredArgsConstructor
public class ProdOptionChangeController {
    private final ProdOptionChangeService prodOptionChangeService;
    @GetMapping("/list") public List<ProdOptionChange> selectList(@RequestParam Map<String, Object> params) { return prodOptionChangeService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return prodOptionChangeService.selectCount(params); }
    @GetMapping public ProdOptionChange select(@RequestParam Map<String, Object> params) { return prodOptionChangeService.select(params); }
    @PostMapping public int insert(@RequestBody ProdOptionChange prodOptionChange) { return prodOptionChangeService.insert(prodOptionChange); }
    @PutMapping public int update(@RequestBody ProdOptionChange prodOptionChange) { return prodOptionChangeService.update(prodOptionChange); }
    @DeleteMapping public int delete(@RequestBody ProdOptionChange prodOptionChange) { return prodOptionChangeService.delete(prodOptionChange); }
}
