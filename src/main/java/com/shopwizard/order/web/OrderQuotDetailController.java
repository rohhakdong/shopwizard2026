package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderQuotDetail;
import com.shopwizard.order.service.OrderQuotDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/quot-detail")
@RequiredArgsConstructor
public class OrderQuotDetailController {
    private final OrderQuotDetailService orderQuotDetailService;
    @GetMapping("/list") public List<OrderQuotDetail> selectList(@RequestParam Map<String, Object> params) { return orderQuotDetailService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return orderQuotDetailService.selectCount(params); }
    @GetMapping public OrderQuotDetail select(@RequestParam Map<String, Object> params) { return orderQuotDetailService.select(params); }
    @GetMapping("/max") public int selectMax(@RequestParam Map<String, Object> params) { return orderQuotDetailService.selectMax(params); }
    @PostMapping public int insert(@RequestBody OrderQuotDetail orderQuotDetail) { return orderQuotDetailService.insert(orderQuotDetail); }
    @PutMapping public int update(@RequestBody OrderQuotDetail orderQuotDetail) { return orderQuotDetailService.update(orderQuotDetail); }
    @DeleteMapping public int delete(@RequestBody OrderQuotDetail orderQuotDetail) { return orderQuotDetailService.delete(orderQuotDetail); }
}
