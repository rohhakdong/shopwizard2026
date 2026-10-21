package com.shopwizard.stock.web;

import com.shopwizard.stock.model.DailyStockState;
import com.shopwizard.stock.service.DailyStockStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stock/daily-stock-state")
@RequiredArgsConstructor
public class DailyStockStateController {
    private final DailyStockStateService dailyStockStateService;
    @GetMapping("/list") public List<DailyStockState> selectList(@RequestParam Map<String, Object> params) { return dailyStockStateService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return dailyStockStateService.selectCount(params); }
    @GetMapping("/check-list") public List<DailyStockState> selectListCheck(@RequestParam Map<String, Object> params) { return dailyStockStateService.selectListCheck(params); }
    @GetMapping("/check-count") public int selectCountCheck(@RequestParam Map<String, Object> params) { return dailyStockStateService.selectCountCheck(params); }
    @GetMapping("/ship-direct-list") public List<DailyStockState> selectListShipDirect(@RequestParam Map<String, Object> params) { return dailyStockStateService.selectListShipDirect(params); }
    @GetMapping public DailyStockState select(@RequestParam Map<String, Object> params) { return dailyStockStateService.select(params); }
}
