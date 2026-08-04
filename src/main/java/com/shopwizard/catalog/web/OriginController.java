package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.Origin;
import com.shopwizard.catalog.service.OriginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/catalog/origin")
@RequiredArgsConstructor
public class OriginController {
    private final OriginService originService;

    @GetMapping("/list") public List<Origin> selectList(@RequestParam Map<String, Object> params) { return originService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return originService.selectCount(params); }
    @GetMapping public Origin select(@RequestParam Map<String, Object> params) { return originService.select(params); }
    @PostMapping public int insert(@RequestBody Origin origin) { return originService.insert(origin); }
    @PutMapping public int update(@RequestBody Origin origin) { return originService.update(origin); }
    @DeleteMapping public int delete(@RequestBody Map<String, Object> params) { return originService.delete(params); }
}
