package com.shopwizard.order.web;

import com.shopwizard.order.model.ProdCodeMatch;
import com.shopwizard.order.service.ProdCodeMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/prod-code-match")
@RequiredArgsConstructor
public class ProdCodeMatchController {
    private final ProdCodeMatchService prodCodeMatchService;
    @GetMapping("/list") public List<ProdCodeMatch> selectList(@RequestParam Map<String, Object> params) { return prodCodeMatchService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return prodCodeMatchService.selectCount(params); }
    @GetMapping public ProdCodeMatch select(@RequestParam Map<String, Object> params) { return prodCodeMatchService.select(params); }
    @PostMapping public int insert(@RequestBody ProdCodeMatch prodCodeMatch) { return prodCodeMatchService.insert(prodCodeMatch); }
    @PutMapping public int update(@RequestBody ProdCodeMatch prodCodeMatch) { return prodCodeMatchService.update(prodCodeMatch); }
    @DeleteMapping public int delete(@RequestBody ProdCodeMatch prodCodeMatch) { return prodCodeMatchService.delete(prodCodeMatch); }
}
