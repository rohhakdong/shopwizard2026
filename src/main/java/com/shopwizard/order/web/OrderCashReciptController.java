package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderCashRecipt;
import com.shopwizard.order.service.OrderCashReciptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/ordercashrecipt")
public class OrderCashReciptController {
    private final OrderCashReciptService orderCashReciptService;

    @GetMapping("/list")
    public List<OrderCashRecipt> selectList(@RequestParam Map<String, Object> params) { return orderCashReciptService.selectList(params); }

    @GetMapping
    public OrderCashRecipt select(@RequestParam Map<String, Object> params) { return orderCashReciptService.select(params); }

}
