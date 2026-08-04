package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderQuot;
import com.shopwizard.order.service.OrderQuotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/quot")
@RequiredArgsConstructor
public class OrderQuotController {
    private final OrderQuotService orderQuotService;
    @GetMapping("/list") public List<OrderQuot> selectList(@RequestParam Map<String, Object> params) { return orderQuotService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return orderQuotService.selectCount(params); }
    @GetMapping public OrderQuot select(@RequestParam Map<String, Object> params) { return orderQuotService.select(params); }
    @PostMapping public int insert(@RequestBody OrderQuot orderQuot) { return orderQuotService.insert(orderQuot); }
    @PutMapping public int update(@RequestBody OrderQuot orderQuot) { return orderQuotService.update(orderQuot); }
    @DeleteMapping public int delete(@RequestBody OrderQuot orderQuot) { return orderQuotService.delete(orderQuot); }
}
