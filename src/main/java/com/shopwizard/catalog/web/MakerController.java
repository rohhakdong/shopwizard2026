package com.shopwizard.catalog.web;

import com.shopwizard.catalog.model.Maker;
import com.shopwizard.catalog.service.MakerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/catalog/maker")
@RequiredArgsConstructor
public class MakerController {
    private final MakerService makerService;

    @GetMapping("/list") public List<Maker> selectList(@RequestParam Map<String, Object> params) { return makerService.selectList(params); }
    @GetMapping("/count") public int selectCount(@RequestParam Map<String, Object> params) { return makerService.selectCount(params); }
    @GetMapping public Maker select(@RequestParam Map<String, Object> params) { return makerService.select(params); }
    @PostMapping public int insert(@RequestBody Maker maker) { return makerService.insert(maker); }
    @PutMapping public int update(@RequestBody Maker maker) { return makerService.update(maker); }
    @DeleteMapping public int delete(@RequestBody Map<String, Object> params) { return makerService.delete(params); }
}
