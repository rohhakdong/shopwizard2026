package com.shopwizard.adjust.web;

import com.shopwizard.adjust.model.Adjust;
import com.shopwizard.adjust.model.AdjustDetail;
import com.shopwizard.adjust.service.AdjustDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/adjust/detail")
public class AdjustDetailController {

    private final AdjustDetailService adjustDetailService;

    @GetMapping("/list")
    public List<AdjustDetail> selectList(@RequestParam Map<String, Object> params) { return adjustDetailService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return adjustDetailService.selectCount(params); }

    @GetMapping("/sumbranch/list")
    public List<Adjust> selectListSumBranch(@RequestParam Map<String, Object> params) { return adjustDetailService.selectListSumBranch(params); }

    @GetMapping("/sumshop/list")
    public List<Adjust> selectListSumShop(@RequestParam Map<String, Object> params) { return adjustDetailService.selectListSumShop(params); }

    @GetMapping
    public AdjustDetail select(@RequestParam Map<String, Object> params) { return adjustDetailService.select(params); }

    @PostMapping
    public void insert(@RequestBody AdjustDetail adjustDetail) { adjustDetailService.insert(adjustDetail); }

    @PutMapping
    public void update(@RequestBody AdjustDetail adjustDetail) { adjustDetailService.update(adjustDetail); }

    @DeleteMapping
    public void delete(@RequestBody AdjustDetail adjustDetail) { adjustDetailService.delete(adjustDetail); }

    @PostMapping("/list")
    public void insertList(@RequestBody Map<String, Object> params) { adjustDetailService.insertList(params); }

    @PostMapping("/listbyorder")
    public void insertListByOrder(@RequestBody Map<String, Object> params) { adjustDetailService.insertListByOrder(params); }

    @PostMapping("/schedule")
    public void insertSchedule(@RequestBody Map<String, Object> params) { adjustDetailService.insertSchedule(params); }

    @PutMapping("/list")
    public void updateList(@RequestBody Map<String, Object> params) { adjustDetailService.updateList(params); }

    @PutMapping("/branchsale")
    public void updateBranchSale(@RequestBody Map<String, Object> params) { adjustDetailService.updateBranchSale(params); }

    @PutMapping("/shopbuy")
    public void updateShopBuy(@RequestBody Map<String, Object> params) { adjustDetailService.updateShopBuy(params); }
}
