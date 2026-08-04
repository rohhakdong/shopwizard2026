package com.shopwizard.adjust.web;

import com.shopwizard.adjust.model.AdjustPeriod;
import com.shopwizard.adjust.service.AdjustPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/adjust/period")
public class AdjustPeriodController {

    private final AdjustPeriodService adjustPeriodService;

    @GetMapping("/list")
    public List<AdjustPeriod> selectList(@RequestParam Map<String, Object> params) { return adjustPeriodService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return adjustPeriodService.selectCount(params); }

    @GetMapping
    public AdjustPeriod select(@RequestParam Map<String, Object> params) { return adjustPeriodService.select(params); }

    @PostMapping
    public void insert(@RequestBody AdjustPeriod adjustPeriod) { adjustPeriodService.insert(adjustPeriod); }

    @PutMapping
    public void update(@RequestBody AdjustPeriod adjustPeriod) { adjustPeriodService.update(adjustPeriod); }

    @DeleteMapping
    public void delete(@RequestBody AdjustPeriod adjustPeriod) { adjustPeriodService.delete(adjustPeriod); }
}
