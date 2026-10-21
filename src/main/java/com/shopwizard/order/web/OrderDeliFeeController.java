package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderDeliFee;
import com.shopwizard.order.service.OrderDeliFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/orderdelivee")
public class OrderDeliFeeController {
    private final OrderDeliFeeService orderDeliFeeService;

    @GetMapping("/list")
    public List<OrderDeliFee> selectList(@RequestParam Map<String, Object> params) { return orderDeliFeeService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return orderDeliFeeService.selectCount(params); }

    @GetMapping
    public OrderDeliFee select(@RequestParam Map<String, Object> params) { return orderDeliFeeService.select(params); }

}
