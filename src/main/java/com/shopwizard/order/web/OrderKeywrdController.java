package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderKeywrd;
import com.shopwizard.order.service.OrderKeywrdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/keywrd")
@RequiredArgsConstructor
public class OrderKeywrdController {
    private final OrderKeywrdService orderKeywrdService;
    @GetMapping("/list") public List<OrderKeywrd> selectList(@RequestParam Map<String, Object> params) { return orderKeywrdService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return orderKeywrdService.selectCount(params); }
    @GetMapping public OrderKeywrd select(@RequestParam Map<String, Object> params) { return orderKeywrdService.select(params); }
}
