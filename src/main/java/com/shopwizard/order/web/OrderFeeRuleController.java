package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderFeeRule;
import com.shopwizard.order.service.OrderFeeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/fee-rule")
@RequiredArgsConstructor
public class OrderFeeRuleController {
    private final OrderFeeRuleService orderFeeRuleService;
    @GetMapping("/list") public List<OrderFeeRule> selectList(@RequestParam Map<String, Object> params) { return orderFeeRuleService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return orderFeeRuleService.selectCount(params); }
    @GetMapping public OrderFeeRule select(@RequestParam Map<String, Object> params) { return orderFeeRuleService.select(params); }
    @PostMapping public int insert(@RequestBody OrderFeeRule orderFeeRule) { return orderFeeRuleService.insert(orderFeeRule); }
    @PutMapping public int update(@RequestBody OrderFeeRule orderFeeRule) { return orderFeeRuleService.update(orderFeeRule); }
    @DeleteMapping public int delete(@RequestBody OrderFeeRule orderFeeRule) { return orderFeeRuleService.delete(orderFeeRule); }
}
