package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderProdCode;
import com.shopwizard.order.service.OrderProdCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/prod-code")
@RequiredArgsConstructor
public class OrderProdCodeController {
    private final OrderProdCodeService orderProdCodeService;
    @GetMapping("/list") public List<OrderProdCode> selectList(@RequestParam Map<String, Object> params) { return orderProdCodeService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return orderProdCodeService.selectCount(params); }
    @GetMapping public OrderProdCode select(@RequestParam Map<String, Object> params) { return orderProdCodeService.select(params); }
    @PostMapping public int insert(@RequestBody OrderProdCode orderProdCode) { return orderProdCodeService.insert(orderProdCode); }
    @PutMapping public int update(@RequestBody OrderProdCode orderProdCode) { return orderProdCodeService.update(orderProdCode); }
    @DeleteMapping public int delete(@RequestBody OrderProdCode orderProdCode) { return orderProdCodeService.delete(orderProdCode); }
}
