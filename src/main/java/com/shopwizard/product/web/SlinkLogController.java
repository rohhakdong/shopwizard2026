package com.shopwizard.product.web;

import com.shopwizard.product.model.SlinkLog;
import com.shopwizard.product.service.SlinkLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/slinklog")
public class SlinkLogController {

    private final SlinkLogService slinkLogService;

    @GetMapping("/list")
    public List<SlinkLog> selectList(@RequestParam Map<String, Object> params) { return slinkLogService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return slinkLogService.selectCount(params); }

    @GetMapping
    public SlinkLog select(@RequestParam Map<String, Object> params) { return slinkLogService.select(params); }

    @PostMapping
    public void insert(@RequestBody SlinkLog slinkLog) { slinkLogService.insert(slinkLog); }

    @PutMapping
    public void update(@RequestBody SlinkLog slinkLog) { slinkLogService.update(slinkLog); }

    @DeleteMapping
    public void delete(@RequestBody SlinkLog slinkLog) { slinkLogService.delete(slinkLog); }
}
