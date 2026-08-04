package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderPayLog;
import com.shopwizard.order.service.OrderPayLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/orderpaylog")
public class OrderPayLogController {
    private final OrderPayLogService orderPayLogService;

    @GetMapping("/list")
    public List<OrderPayLog> selectList(@RequestParam Map<String, Object> params) { return orderPayLogService.selectList(params); }

    @GetMapping("/{logId}")
    public OrderPayLog select(@PathVariable Integer logId) { return orderPayLogService.select(logId); }
}
