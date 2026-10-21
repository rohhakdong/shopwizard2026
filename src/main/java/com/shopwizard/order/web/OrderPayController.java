package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderPay;
import com.shopwizard.order.service.OrderPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/orderpay")
public class OrderPayController {
    private final OrderPayService orderPayService;

    @GetMapping("/list")
    public List<OrderPay> selectList(@RequestParam Map<String, Object> params) { return orderPayService.selectList(params); }

    @GetMapping
    public OrderPay select(@RequestParam Map<String, Object> params) { return orderPayService.select(params); }

}
