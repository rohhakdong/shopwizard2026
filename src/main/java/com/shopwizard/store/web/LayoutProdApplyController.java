package com.shopwizard.store.web;

import com.shopwizard.store.model.LayoutProdApply;
import com.shopwizard.store.service.LayoutProdApplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/layoutprodapply")
@RequiredArgsConstructor
public class LayoutProdApplyController {
    private final LayoutProdApplyService layoutProdApplyService;

    @GetMapping("/list")
    public List<LayoutProdApply> selectList(@RequestParam Map<String, Object> params) {
        return layoutProdApplyService.selectList(params);
    }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) {
        return layoutProdApplyService.selectCount(params);
    }

    @PostMapping
    public int insert(@RequestBody LayoutProdApply layoutProdApply) {
        return layoutProdApplyService.insert(layoutProdApply);
    }

    @PutMapping
    public int update(@RequestBody LayoutProdApply layoutProdApply) {
        return layoutProdApplyService.update(layoutProdApply);
    }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) {
        return layoutProdApplyService.delete(params);
    }
}
