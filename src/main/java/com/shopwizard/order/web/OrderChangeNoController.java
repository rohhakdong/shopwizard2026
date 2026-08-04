package com.shopwizard.order.web;

import com.shopwizard.order.service.OrderChangeNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/orderchangeno")
public class OrderChangeNoController {
    private final OrderChangeNoService orderChangeNoService;

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return orderChangeNoService.selectCount(params); }

    @GetMapping("/max")
    public int selectMax(@RequestParam Map<String, Object> params) { return orderChangeNoService.selectMax(params); }

    @GetMapping
    public Integer selectChangeNo(@RequestParam Map<String, Object> params) { return orderChangeNoService.selectChangeNo(params); }

    @PostMapping
    public void insert(@RequestBody Map<String, Object> params) { orderChangeNoService.insert(params); }
}
