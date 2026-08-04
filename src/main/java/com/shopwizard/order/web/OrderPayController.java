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

    @PostMapping
    public void insert(@RequestBody OrderPay orderPay) { orderPayService.insert(orderPay); }

    @PutMapping
    public void update(@RequestBody OrderPay orderPay) { orderPayService.update(orderPay); }

    @PutMapping("/reciept")
    public void updateOrderPayRecipt(@RequestBody Map<String, Object> params) { orderPayService.updateOrderPayRecipt(params); }

    @PutMapping("/reciept/auto")
    public void updateOrderPayReciptAuto(@RequestBody Map<String, Object> params) { orderPayService.updateOrderPayReciptAuto(params); }

    @DeleteMapping
    public void delete(@RequestBody Map<String, Object> params) { orderPayService.delete(params); }
}
