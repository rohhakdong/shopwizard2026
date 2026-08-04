package com.shopwizard.profile.web;

import com.shopwizard.profile.model.Comp;
import com.shopwizard.profile.service.CompService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/comp")
public class CompController {

    private final CompService compService;

    @GetMapping("/list")
    public List<Comp> selectList(@RequestParam Map<String, Object> params) { return compService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return compService.selectCount(params); }

    @GetMapping
    public Comp select(@RequestParam Map<String, Object> params) { return compService.select(params); }

    @PostMapping
    public int insert(@RequestBody Comp comp) { return compService.insert(comp); }

    @PutMapping
    public int update(@RequestBody Comp comp) { return compService.update(comp); }

    @DeleteMapping("/{compCode}")
    public int delete(@PathVariable String compCode) { return compService.delete(compCode); }
}
