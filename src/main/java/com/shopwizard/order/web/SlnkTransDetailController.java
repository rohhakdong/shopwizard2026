package com.shopwizard.order.web;

import com.shopwizard.order.model.SlnkTransDetail;
import com.shopwizard.order.service.SlnkTransDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/slnktransdetail")
public class SlnkTransDetailController {
    private final SlnkTransDetailService slnkTransDetailService;

    @GetMapping("/list")
    public List<SlnkTransDetail> selectList(@RequestParam Map<String, Object> params) { return slnkTransDetailService.selectList(params); }

    @GetMapping
    public SlnkTransDetail select(@RequestParam Map<String, Object> params) { return slnkTransDetailService.select(params); }

    @PostMapping
    public void insert(@RequestBody SlnkTransDetail slnkTransDetail) { slnkTransDetailService.insert(slnkTransDetail); }

    @PutMapping
    public void update(@RequestBody SlnkTransDetail slnkTransDetail) { slnkTransDetailService.update(slnkTransDetail); }

    @DeleteMapping
    public void delete(@RequestBody Map<String, Object> params) { slnkTransDetailService.delete(params); }
}
