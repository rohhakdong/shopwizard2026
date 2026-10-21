package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderWork;
import com.shopwizard.order.service.OrderWorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/work")
@RequiredArgsConstructor
public class OrderWorkController {
    private final OrderWorkService orderWorkService;
    @GetMapping("/list") public List<OrderWork> selectList(@RequestParam Map<String, Object> params) { return orderWorkService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return orderWorkService.selectCount(params); }
    @GetMapping public OrderWork select(@RequestParam Map<String, Object> params) { return orderWorkService.select(params); }
}
