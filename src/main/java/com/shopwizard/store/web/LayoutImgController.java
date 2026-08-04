package com.shopwizard.store.web;

import com.shopwizard.store.model.LayoutImg;
import com.shopwizard.store.service.LayoutImgService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/layoutimg")
@RequiredArgsConstructor
public class LayoutImgController {
    private final LayoutImgService layoutImgService;

    @GetMapping("/list")
    public List<LayoutImg> selectList(@RequestParam Map<String, Object> params) {
        return layoutImgService.selectList(params);
    }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) {
        return layoutImgService.selectCount(params);
    }

    @GetMapping
    public LayoutImg select(@RequestParam Map<String, Object> params) {
        return layoutImgService.select(params);
    }

    @PostMapping
    public int insert(@RequestBody LayoutImg layoutImg) {
        return layoutImgService.insert(layoutImg);
    }

    @PutMapping
    public int update(@RequestBody LayoutImg layoutImg) {
        return layoutImgService.update(layoutImg);
    }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) {
        return layoutImgService.delete(params);
    }
}
