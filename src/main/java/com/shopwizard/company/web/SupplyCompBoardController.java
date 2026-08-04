package com.shopwizard.company.web;

import com.shopwizard.company.model.SupplyCompBoard;
import com.shopwizard.company.service.SupplyCompBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company/supply-comp-board")
@RequiredArgsConstructor
public class SupplyCompBoardController {
    private final SupplyCompBoardService supplyCompBoardService;

    @GetMapping
    public List<SupplyCompBoard> getList(@RequestParam Map<String, Object> params) {
        return supplyCompBoardService.getList(params);
    }

    @GetMapping("/count")
    public int getCount(@RequestParam Map<String, Object> params) {
        return supplyCompBoardService.getCount(params);
    }

    @GetMapping("/{boardNo}")
    public SupplyCompBoard get(@PathVariable int boardNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("boardNo", boardNo);
        return supplyCompBoardService.get(params);
    }

    @PostMapping
    public int insert(@RequestBody SupplyCompBoard supplyCompBoard) {
        return supplyCompBoardService.insert(supplyCompBoard);
    }

    @PutMapping
    public int update(@RequestBody SupplyCompBoard supplyCompBoard) {
        return supplyCompBoardService.update(supplyCompBoard);
    }

    @DeleteMapping
    public int delete(@RequestBody SupplyCompBoard supplyCompBoard) {
        return supplyCompBoardService.delete(supplyCompBoard);
    }
}
