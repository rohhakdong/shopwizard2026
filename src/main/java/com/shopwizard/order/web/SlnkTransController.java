package com.shopwizard.order.web;

import com.shopwizard.order.model.SlnkTrans;
import com.shopwizard.order.service.SlnkTransService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/slnktrans")
public class SlnkTransController {
    private final SlnkTransService slnkTransService;

    @GetMapping("/list")
    public List<SlnkTrans> selectList(@RequestParam Map<String, Object> params) { return slnkTransService.selectList(params); }

    @GetMapping
    public SlnkTrans select(@RequestParam Map<String, Object> params) { return slnkTransService.select(params); }

    @PostMapping
    public void insert(@RequestBody SlnkTrans slnkTrans) { slnkTransService.insert(slnkTrans); }

    @PutMapping
    public void update(@RequestBody SlnkTrans slnkTrans) { slnkTransService.update(slnkTrans); }

    @DeleteMapping
    public void delete(@RequestBody Map<String, Object> params) { slnkTransService.delete(params); }
}
