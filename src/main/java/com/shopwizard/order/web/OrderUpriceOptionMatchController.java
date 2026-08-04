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
    @PostMapping public int insert(@RequestBody OrderUpriceOptionMatch orderUpriceOptionMatch) { return orderUpriceOptionMatchService.insert(orderUpriceOptionMatch); }
    @PutMapping public int update(@RequestBody OrderUpriceOptionMatch orderUpriceOptionMatch) { return orderUpriceOptionMatchService.update(orderUpriceOptionMatch); }
    @DeleteMapping public int delete(@RequestBody OrderUpriceOptionMatch orderUpriceOptionMatch) { return orderUpriceOptionMatchService.delete(orderUpriceOptionMatch); }
}
