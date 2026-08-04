package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderUprice;
import com.shopwizard.order.service.OrderUpriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/uprice")
@RequiredArgsConstructor
public class OrderUpriceController {
    private final OrderUpriceService orderUpriceService;
    @GetMapping("/list") public List<OrderUprice> selectList(@RequestParam Map<String, Object> params) { return orderUpriceService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return orderUpriceService.selectCount(params); }
    @GetMapping public OrderUprice select(@RequestParam Map<String, Object> params) { return orderUpriceService.select(params); }
    @PostMapping public int insert(@RequestBody OrderUprice orderUprice) { return orderUpriceService.insert(orderUprice); }
    @PutMapping public int update(@RequestBody OrderUprice orderUprice) { return orderUpriceService.update(orderUprice); }
    @DeleteMapping public int delete(@RequestBody OrderUprice orderUprice) { return orderUpriceService.delete(orderUprice); }
}
