package com.shopwizard.store.web;

import com.shopwizard.store.model.Design;
import com.shopwizard.store.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/design")
@RequiredArgsConstructor
public class DesignController {
    private final DesignService designService;

    @GetMapping("/list")
    public List<Design> selectList(@RequestParam Map<String, Object> params) {
        return designService.selectList(params);
    }

    @PostMapping
    public int insert(@RequestBody Design design) {
        return designService.insert(design);
    }

    @PutMapping
    public int update(@RequestBody Design design) {
        return designService.update(design);
    }

    @DeleteMapping("/{designId}")
    public int delete(@PathVariable Integer designId) {
        return designService.delete(designId);
    }
}
