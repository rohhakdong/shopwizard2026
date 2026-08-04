package com.shopwizard.store.web;

import com.shopwizard.store.model.Point;
import com.shopwizard.store.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/point")
@RequiredArgsConstructor
public class PointController {
    private final PointService pointService;

    @GetMapping("/list")
    public List<Point> selectList(@RequestParam Map<String, Object> params) {
        return pointService.selectList(params);
    }

    @GetMapping("/{pointId}")
    public Point select(@PathVariable Integer pointId) {
        return pointService.select(pointId);
    }

    @PostMapping
    public int insert(@RequestBody Point point) {
        return pointService.insert(point);
    }

    @PutMapping
    public int update(@RequestBody Point point) {
        return pointService.update(point);
    }

    @DeleteMapping("/{pointId}")
    public int delete(@PathVariable Integer pointId) {
        return pointService.delete(pointId);
    }
}
