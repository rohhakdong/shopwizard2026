package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderUpriceMatch;
import com.shopwizard.order.service.OrderUpriceMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/uprice-match")
@RequiredArgsConstructor
public class OrderUpriceMatchController {
    private final OrderUpriceMatchService orderUpriceMatchService;
    @GetMapping("/list") public List<OrderUpriceMatch> selectList(@RequestParam Map<String, Object> params) { return orderUpriceMatchService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return orderUpriceMatchService.selectCount(params); }
    @GetMapping public OrderUpriceMatch select(@RequestParam Map<String, Object> params) { return orderUpriceMatchService.select(params); }
    @PostMapping public int insert(@RequestBody OrderUpriceMatch orderUpriceMatch) { return orderUpriceMatchService.insert(orderUpriceMatch); }
    @PutMapping public int update(@RequestBody OrderUpriceMatch orderUpriceMatch) { return orderUpriceMatchService.update(orderUpriceMatch); }
    @DeleteMapping public int delete(@RequestBody OrderUpriceMatch orderUpriceMatch) { return orderUpriceMatchService.delete(orderUpriceMatch); }
}
