package com.shopwizard.profile.web;

import com.shopwizard.profile.model.CompStoreProd;
import com.shopwizard.profile.service.CompStoreProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/compStoreProd")
public class CompStoreProdController {

    private final CompStoreProdService compStoreProdService;

    @GetMapping("/list")
    public List<CompStoreProd> selectList(@RequestParam Map<String, Object> params) { return compStoreProdService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return compStoreProdService.selectCount(params); }

    @GetMapping
    public CompStoreProd select(@RequestParam Map<String, Object> params) { return compStoreProdService.select(params); }

    @PostMapping
    public int insert(@RequestBody CompStoreProd compStoreProd) { return compStoreProdService.insert(compStoreProd); }

    @PutMapping
    public int update(@RequestBody CompStoreProd compStoreProd) { return compStoreProdService.update(compStoreProd); }

    @DeleteMapping
    public int delete(@RequestParam Map<String, Object> params) { return compStoreProdService.delete(params); }
}
