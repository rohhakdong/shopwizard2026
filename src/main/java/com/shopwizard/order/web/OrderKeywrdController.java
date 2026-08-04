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
    @PostMapping public int insert(@RequestBody OrderKeywrd orderKeywrd) { return orderKeywrdService.insert(orderKeywrd); }
    @PutMapping public int update(@RequestBody OrderKeywrd orderKeywrd) { return orderKeywrdService.update(orderKeywrd); }
    @DeleteMapping public int delete(@RequestBody OrderKeywrd orderKeywrd) { return orderKeywrdService.delete(orderKeywrd); }
}
