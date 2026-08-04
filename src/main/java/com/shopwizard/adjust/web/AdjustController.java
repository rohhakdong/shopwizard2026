package com.shopwizard.adjust.web;

import com.shopwizard.adjust.model.Adjust;
import com.shopwizard.adjust.service.AdjustService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/adjust/adjust")
public class AdjustController {

    private final AdjustService adjustService;

    @GetMapping("/list")
    public List<Adjust> selectList(@RequestParam Map<String, Object> params) { return adjustService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return adjustService.selectCount(params); }

    @GetMapping("/branch/list")
    public List<Adjust> selectListBranchSale(@RequestParam Map<String, Object> params) { return adjustService.selectListBranchSale(params); }

    @GetMapping("/branch/count")
    public int selectCountBranchSale(@RequestParam Map<String, Object> params) { return adjustService.selectCountBranchSale(params); }

    @GetMapping("/shop/list")
    public List<Adjust> selectListShopBuy(@RequestParam Map<String, Object> params) { return adjustService.selectListShopBuy(params); }

    @GetMapping("/shop/count")
    public int selectCountShopBuy(@RequestParam Map<String, Object> params) { return adjustService.selectCountShopBuy(params); }

    @GetMapping
    public Adjust select(@RequestParam Map<String, Object> params) { return adjustService.select(params); }

    @PostMapping
    public void insert(@RequestBody Adjust adjust) { adjustService.insert(adjust); }

    @PutMapping
    public void update(@RequestBody Adjust adjust) { adjustService.update(adjust); }

    @DeleteMapping
    public void delete(@RequestBody Adjust adjust) { adjustService.delete(adjust); }
}
