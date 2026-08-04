package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderSoclQuot;
import com.shopwizard.order.service.OrderSoclQuotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/socl-quot")
@RequiredArgsConstructor
public class OrderSoclQuotController {
    private final OrderSoclQuotService orderSoclQuotService;
    @GetMapping("/list") public List<OrderSoclQuot> selectList(@RequestParam Map<String, Object> params) { return orderSoclQuotService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return orderSoclQuotService.selectCount(params); }
    @GetMapping public OrderSoclQuot select(@RequestParam Map<String, Object> params) { return orderSoclQuotService.select(params); }
    @PostMapping public int insert(@RequestBody OrderSoclQuot orderSoclQuot) { return orderSoclQuotService.insert(orderSoclQuot); }
    @PutMapping public int update(@RequestBody OrderSoclQuot orderSoclQuot) { return orderSoclQuotService.update(orderSoclQuot); }
    @DeleteMapping public int delete(@RequestBody OrderSoclQuot orderSoclQuot) { return orderSoclQuotService.delete(orderSoclQuot); }
}
