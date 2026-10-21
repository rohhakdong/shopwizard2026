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
    private final com.shopwizard.framework.auth.DataScopeGuard scopeGuard;   // 상점/채널 제한 관리자는 자기 정산만(목록은 DataScopeFilter+매퍼 조건)

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
    public Adjust select(@RequestParam Map<String, Object> params, jakarta.servlet.http.HttpServletRequest request) {
        scopeGuard.requireAdjust(request, toInt(params.get("adjustNo")));
        return adjustService.select(params);
    }

    @PutMapping
    public void update(@RequestBody Adjust adjust, jakarta.servlet.http.HttpServletRequest request) {
        scopeGuard.requireAdjust(request, adjust.getAdjustNo());
        adjustService.update(adjust);
    }

    private static Integer toInt(Object v) {
        try { return v == null ? null : Integer.valueOf(String.valueOf(v).trim()); } catch (NumberFormatException e) { return null; }
    }

}
