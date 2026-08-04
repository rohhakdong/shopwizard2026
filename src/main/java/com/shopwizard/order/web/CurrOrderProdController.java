package com.shopwizard.order.web;

import com.shopwizard.order.model.CurrOrderProd;
import com.shopwizard.order.service.CurrOrderProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/curr-order-prod")
@RequiredArgsConstructor
public class CurrOrderProdController {
    private final CurrOrderProdService currOrderProdService;
    @GetMapping("/list") public List<CurrOrderProd> selectList(@RequestParam Map<String, Object> params) { return currOrderProdService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return currOrderProdService.selectCount(params); }
    @GetMapping public CurrOrderProd select(@RequestParam Map<String, Object> params) { return currOrderProdService.select(params); }
    @PostMapping public int insert(@RequestBody CurrOrderProd currOrderProd) { return currOrderProdService.insert(currOrderProd); }
    @PostMapping("/matching") public int insertMatching(@RequestBody Map<String, Object> params) { return currOrderProdService.insertMatching(params); }
    @PutMapping public int update(@RequestBody CurrOrderProd currOrderProd) { return currOrderProdService.update(currOrderProd); }
    @DeleteMapping public int delete(@RequestBody CurrOrderProd currOrderProd) { return currOrderProdService.delete(currOrderProd); }
}
