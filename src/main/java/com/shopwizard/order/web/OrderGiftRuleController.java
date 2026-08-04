package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderGiftRule;
import com.shopwizard.order.service.OrderGiftRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/gift-rule")
@RequiredArgsConstructor
public class OrderGiftRuleController {
    private final OrderGiftRuleService orderGiftRuleService;
    @GetMapping("/list") public List<OrderGiftRule> selectList(@RequestParam Map<String, Object> params) { return orderGiftRuleService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return orderGiftRuleService.selectCount(params); }
    @GetMapping public OrderGiftRule select(@RequestParam Map<String, Object> params) { return orderGiftRuleService.select(params); }
    @PostMapping public int insert(@RequestBody OrderGiftRule orderGiftRule) { return orderGiftRuleService.insert(orderGiftRule); }
    @PutMapping public int update(@RequestBody OrderGiftRule orderGiftRule) { return orderGiftRuleService.update(orderGiftRule); }
    @DeleteMapping public int delete(@RequestBody OrderGiftRule orderGiftRule) { return orderGiftRuleService.delete(orderGiftRule); }
}
