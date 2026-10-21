package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderProdApprovLine;
import com.shopwizard.order.service.OrderProdApprovLineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/orderprodapprovline")
public class OrderProdApprovLineController {
    private final OrderProdApprovLineService orderProdApprovLineService;

    @GetMapping("/list")
    public List<OrderProdApprovLine> selectList(@RequestParam Map<String, Object> params) { return orderProdApprovLineService.selectList(params); }

    @GetMapping("/count")
    public int count(@RequestParam Map<String, Object> params) { return orderProdApprovLineService.count(params); }

    @GetMapping("/count/ready")
    public int countReady(@RequestParam Map<String, Object> params) { return orderProdApprovLineService.countReady(params); }

    @GetMapping("/count/approv")
    public int countApprov(@RequestParam Map<String, Object> params) { return orderProdApprovLineService.countApprov(params); }

    @GetMapping("/count/reject")
    public int countReject(@RequestParam Map<String, Object> params) { return orderProdApprovLineService.countReject(params); }

}
