package com.shopwizard.ship.web;

import com.shopwizard.ship.model.ShipCheck;
import com.shopwizard.ship.service.ShipCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ship/ship-check")
@RequiredArgsConstructor
public class ShipCheckController {
    private final ShipCheckService shipCheckService;
    @GetMapping("/list") public List<ShipCheck> selectList(@RequestParam Map<String, Object> params) { return shipCheckService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return shipCheckService.selectCount(params); }
    @GetMapping public ShipCheck select(@RequestParam Map<String, Object> params) { return shipCheckService.select(params); }
    @PostMapping public int insert(@RequestBody ShipCheck shipCheck) { return shipCheckService.insert(shipCheck); }
    @PutMapping("/print-count") public int updatePrintCount(@RequestBody ShipCheck shipCheck) { return shipCheckService.updatePrintCount(shipCheck); }
    @DeleteMapping public int delete(@RequestBody ShipCheck shipCheck) { return shipCheckService.delete(shipCheck); }
}
