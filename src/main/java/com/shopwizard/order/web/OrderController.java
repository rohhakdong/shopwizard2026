package com.shopwizard.order.web;

import com.shopwizard.order.model.Order;
import com.shopwizard.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/order")
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/list")
    public List<Order> selectList(@RequestParam Map<String, Object> params) { return orderService.selectList(params); }

    @GetMapping("/orderlist")
    public List<Order> selectListOrderList(@RequestParam Map<String, Object> params) { return orderService.selectListOrderList(params); }

    @GetMapping("/orderlist/count")
    public int selectCountOrderList(@RequestParam Map<String, Object> params) { return orderService.selectCountOrderList(params); }

    @GetMapping("/pay/list")
    public List<Order> selectListPay(@RequestParam Map<String, Object> params) { return orderService.selectListPay(params); }

    @GetMapping
    public Order select(@RequestParam Map<String, Object> params) { return orderService.select(params); }

    @PostMapping
    public void insert(@RequestBody Order order) { orderService.insert(order); }

    @PutMapping("/cancel")
    public void updateOrderCancel(@RequestBody Order order) { orderService.updateOrderCancel(order); }

    @PutMapping("/state")
    public void updateOrderState(@RequestBody Map<String, Object> params) { orderService.updateOrderState(params); }

    @PutMapping("/refundcancel")
    public void updateRefundCancel(@RequestBody Map<String, Object> params) { orderService.updateRefundCancel(params); }
}
