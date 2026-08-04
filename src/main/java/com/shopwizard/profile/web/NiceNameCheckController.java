package com.shopwizard.profile.web;

import com.shopwizard.profile.model.NiceNameCheck;
import com.shopwizard.profile.service.NiceNameCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/niceNameCheck")
public class NiceNameCheckController {

    private final NiceNameCheckService niceNameCheckService;

    @GetMapping("/list")
    public List<NiceNameCheck> selectList(@RequestParam Map<String, Object> params) { return niceNameCheckService.selectList(params); }

    @GetMapping("/{nameCheckUid}")
    public NiceNameCheck select(@PathVariable String nameCheckUid) { return niceNameCheckService.select(nameCheckUid); }

    @PostMapping
    public int insert(@RequestBody NiceNameCheck niceNameCheck) { return niceNameCheckService.insert(niceNameCheck); }
}
