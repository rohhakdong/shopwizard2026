package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderProdMatch;
import com.shopwizard.order.service.OrderProdMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/prod-match")
@RequiredArgsConstructor
public class OrderProdMatchController {
    private final OrderProdMatchService orderProdMatchService;
    @GetMapping("/list") public List<OrderProdMatch> selectList(@RequestParam Map<String, Object> params) { return orderProdMatchService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return orderProdMatchService.selectCount(params); }
    @GetMapping public OrderProdMatch select(@RequestParam Map<String, Object> params) { return orderProdMatchService.select(params); }
}
