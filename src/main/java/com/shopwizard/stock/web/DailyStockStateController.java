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
    @PostMapping public int insert(@RequestBody DailyStockState dailyStockState) { return dailyStockStateService.insert(dailyStockState); }
    @PostMapping("/enter-qty") public int insertEnterQty(@RequestBody DailyStockState dailyStockState) { return dailyStockStateService.insertEnterQty(dailyStockState); }
    @PostMapping("/ship-qty") public int insertShipQty(@RequestBody DailyStockState dailyStockState) { return dailyStockStateService.insertShipQty(dailyStockState); }
    @PutMapping public int update(@RequestBody DailyStockState dailyStockState) { return dailyStockStateService.update(dailyStockState); }
    @PutMapping("/enter-qty") public int updateEnterQty(@RequestBody DailyStockState dailyStockState) { return dailyStockStateService.updateEnterQty(dailyStockState); }
    @PutMapping("/ship-qty") public int updateShipQty(@RequestBody DailyStockState dailyStockState) { return dailyStockStateService.updateShipQty(dailyStockState); }
    @PutMapping("/enter-qty-after-diff") public int updateEnterQtyAfterDiff(@RequestBody Map<String, Object> params) { return dailyStockStateService.updateEnterQtyAfterDiff(params); }
    @DeleteMapping public int delete(@RequestBody DailyStockState dailyStockState) { return dailyStockStateService.delete(dailyStockState); }
}
