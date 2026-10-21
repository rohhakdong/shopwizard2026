package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderUpriceOptionMatch;
import com.shopwizard.order.service.OrderUpriceOptionMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/uprice-option-match")
@RequiredArgsConstructor
public class OrderUpriceOptionMatchController {
    private final OrderUpriceOptionMatchService orderUpriceOptionMatchService;
    @GetMapping("/list") public List<OrderUpriceOptionMatch> selectList(@RequestParam Map<String, Object> params) { return orderUpriceOptionMatchService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return orderUpriceOptionMatchService.selectCount(params); }
    @GetMapping public OrderUpriceOptionMatch select(@RequestParam Map<String, Object> params) { return orderUpriceOptionMatchService.select(params); }
}
